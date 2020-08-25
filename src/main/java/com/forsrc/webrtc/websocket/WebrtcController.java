package com.forsrc.webrtc.websocket;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping()
public class WebrtcController {

	private static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	SimpUserRegistry userRegistry;

	@Autowired
	private SimpMessageSendingOperations messagingTemplate;

 
	private List<String> getSessions(String topic) {
		return userRegistry.findSubscriptions((s) -> topic.equals(s.getDestination())).parallelStream()
				.map((s) -> s.getSession().getId()).collect(Collectors.toList());

	}

	@MessageMapping("/room/{room}/join")
	public void join(@DestinationVariable("room") String room, @Headers Map<String, Object> headers,
			@Payload String body, Principal principal, SimpMessageHeaderAccessor headerAccessor)
			throws IOException, InterruptedException {
		String topic = String.format("/topic/room/%s/join", room);

		System.out.println(String.format("user: %s -> room: %s -> body: %s", principal.getName(), room, body));
		Map<String, Object> map = new HashMap<>();
		map.put("session", headerAccessor.getSessionId());
		map.put("allSessions", getSessions(topic));

		messagingTemplate.convertAndSend(String.format("/topic/room/%s/join", room), map);
	}

	@MessageMapping("/session/{toSession}/webrtc")
	public void user(@DestinationVariable("toSession") String toSession, @Headers Map<String, Object> headers,
			@Payload String body, Principal principal, SimpMessageHeaderAccessor headerAccessor)
			throws IOException, InterruptedException {
		if (headerAccessor.getSessionId().equals(toSession)) {
			return;
		}
		System.out.println(String.format("from %s to /session/%s/webrtc -> %s", headerAccessor.getSessionId(), toSession, body));

		Map<String, Object> map = new HashMap<>();
		map.put("session", headerAccessor.getSessionId());
		map.put("body", body);
		
		sendToSession(toSession, String.format("/session/%s/webrtc", toSession), map);
	}
	
	@MessageMapping("/user/session")
	public void sessionId(@Headers Map<String, Object> headers, @Payload String body, Principal principal,
			SimpMessageHeaderAccessor headerAccessor) throws IOException, InterruptedException {
		System.out.println(String.format("session: %s -> to user: %s -> body: %s", headerAccessor.getSessionId(),
				principal.getName(), body));
		sendToSession(headerAccessor.getSessionId(), "/session",
				headerAccessor.getSessionId());
	}

	private void sendToSession(String sessionId, String destination, Object payload) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headerAccessor.setSessionId(sessionId);
		headerAccessor.setLeaveMutable(true);

		messagingTemplate.convertAndSendToUser(sessionId, destination, payload, headerAccessor.getMessageHeaders());
	}
}
