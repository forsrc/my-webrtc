var socket = new SockJS("/websocket");

var client = Stomp.over(socket);

client.connect({
	username : "forsrc",
	password : "forsrc"
}, function(succ) {
	console.log('client connect success:', succ);
	
	client.send("/app/room/room-1", {}, "test room");
	client.send("/app/room/room-1/test", {}, "test room/test");
	client.send("/app/user/forsrc/message", {}, "test user");

	client.subscribe("/topic/room/room-1", function(res) {
		console.log("/topic/room/room-1", res);
	});
	client.subscribe("/topic/room/room-1/test", function(res) {
		console.log("/topic/room/room-1/test", res);
	});

	client.subscribe("/user/message", function(res) {
		console.log('/user/message', res);
	});
}, function(error) {
	console.log('client connect error:', error);
});
