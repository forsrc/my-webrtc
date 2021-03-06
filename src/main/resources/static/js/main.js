var _socket = new SockJS("/websocket");



var _client = Stomp.over(_socket);
_client.debug = null

const urlParams = new URLSearchParams(window.location.search);
const username = urlParams.get('username');

var _user = username || new Date().getTime();
var _room = "room-001";
var _localVideo;
var _sessionId;
var _localStream;
var _pc = {};

var _peerConnectionConfig = {"iceServers":[{"urls":["turn:192.168.2.110:3478"],"username":"forsrc","credential":"forsrc"}],"iceTransportPolicy":"all","iceCandidatePoolSize":"0"};

function getSessionId(socket) {
	var url = socket._transport.url;
    return url.match(/^.*\/[0-9]*\/(.*)\/websocket/)[1];;
}


_socket.onclose = function() {
	Object.keys(_pc).forEach(function (key) {
		leave(key);
	});
};

_client.connect({
	username: _user,
	password: "forsrc"
}, function (succ) {
	console.log('client connect success:', succ);
	_sessionId = getSessionId(_socket);
	console.log("session id: ", getSessionId(_socket));

	_client.subscribe("/user/session", function (res) {
		console.log('/user/session', res);
		if(_sessionId != res.body) {
			console.error("session", _sessionId, res.body);
			_sessionId = res.body;
		}
	});

	_client.send("/app/user/session", {}, "{}");

	call(_client);

}, function (error) {
	console.log('client connect error:', error);
});

async function call(client) {

	_localVideo = document.getElementById('localVideo');


	var constraints = {
		video: true,
		audio: false,
	};

	if (!navigator.mediaDevices.getUserMedia) {
		alert('Your browser does not support getUserMedia API');
		return;
	}
	_localStream = await navigator.mediaDevices.getUserMedia(constraints);
	_localVideo.srcObject = _localStream;

	client.subscribe("/topic/leave", function(res) {
		console.log("/topic/leave", res);
		var session = res.body;
		leave(session);
	});

	client.subscribe("/user/session/" + _sessionId + "/webrtc", async function (res) {
		console.log("/user/session/" + _sessionId + "/webrtc", res);
		var data = JSON.parse(res.body);
		var id = data.session;
		var signal = JSON.parse(data.body);
		//Make sure it's not coming from yourself
		if (id == _sessionId) {
			return;
		}

		if (signal.offer) {
			console.log("signal.offer:", signal.offer);
			await _pc[id].setRemoteDescription(new RTCSessionDescription(signal.offer));
			if (signal.offer.type == 'offer') {
				var answer = await _pc[id].createAnswer();
				await _pc[id].setLocalDescription(answer);

				//socket.emit('signal', id, JSON.stringify({ 'sdp': _pc[id].localDescription }));
				_client.send("/app/session/" + id + "/webrtc", {}, JSON.stringify({
					'answer': answer
				}));

			}
		}
		
		if (signal.answer && signal.answer.type == 'answer') {
			console.log("signal.answer:", signal.answer);
			await _pc[id].setRemoteDescription(new RTCSessionDescription(signal.answer));
		}
		if (signal.ice) {
			console.log("signal.ice:", signal.ice);
			await _pc[id].addIceCandidate(new RTCIceCandidate(signal.ice));
		}
	});
 
	client.subscribe("/topic/room/" + _room + "/join", async function(res) {
		console.log("/topic/room/" + _room + "/join", res);
		var data = JSON.parse(res.body);
		var allSessions = data.allSessions;
		var id = data.session;
		allSessions.forEach(function (clintSessionId) {
			if (_pc[clintSessionId]) {
				return;
			}
			_pc[clintSessionId] = new RTCPeerConnection(_peerConnectionConfig);
			//Wait for their ice candidate       
			_pc[clintSessionId].onicecandidate = function (event) {
				if (event.candidate == null || clintSessionId == _sessionId) {
					return;
				}
				console.log('SENDING ICE');
				//socket.emit('signal', clintSessionId, JSON.stringify({ 'ice': event.candidate }));
				_client.send("/app/session/" + clintSessionId + "/webrtc", {}, JSON.stringify({
					'ice': event.candidate
				}));
				
			}

			//Wait for their video stream
			_pc[clintSessionId].onaddstream = function (event) {
				createRemoteVideo(event.stream, clintSessionId);
			}

			//Add the local video stream
			_pc[clintSessionId].addStream(_localStream);
			



		});
		


		//Create an offer to connect with your local description
		if (allSessions.length >= 2) {
			console.log('await _pc[id].createOffer()');
			var offer = await _pc[id].createOffer();
			await _pc[id].setLocalDescription(offer);
			// console.log(_pc);
			//socket.emit('signal', id, JSON.stringify({ 'sdp': _pc[id].localDescription }));
			_client.send("/app/session/" + id + "/webrtc", {}, JSON.stringify({
				'offer': offer
			}));
		}
	});

	_client.send("/app/room/" + _room + "/join", {}, "{}");
}




function leave(sessionId) {
	var video = document.querySelector('[data-socket="' + sessionId + '"]');
	if (video) {
		var parentDiv = video.parentElement;
		video.parentElement.parentElement.removeChild(parentDiv);
	}
	if (_pc[sessionId]) {
		_pc[sessionId].close();
		_pc[sessionId] = null;
		delete _pc[sessionId];
	}
};

function createRemoteVideo(stream, sessionId) {

	var video = document.createElement('video');
	var div = document.createElement('div');

	video.setAttribute('data-socket', sessionId);
	video.srcObject = stream;
	video.autoplay = true;
	video.muted = true;
	video.playsinline = true;

	div.appendChild(video);
	document.querySelector('.videos').appendChild(div);
}

