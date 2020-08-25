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

//@Controller
@RequestMapping
public class TestController {

	private static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	SimpUserRegistry userRegistry;

	@Autowired
	private SimpMessageSendingOperations messagingTemplate;

	private List<String> getUsers(String topic) {
		return userRegistry.findSubscriptions((s) -> topic.equals(s.getDestination())).parallelStream()
				.map((s) -> s.getSession().getUser().getName()).collect(Collectors.toList());

	}

	private List<String> getSessions(String topic) {
		return userRegistry.findSubscriptions((s) -> topic.equals(s.getDestination())).parallelStream()
				.map((s) -> s.getSession().getId()).collect(Collectors.toList());

	}

	@MessageMapping("/test/room/{room}/join")
	public void join(@DestinationVariable("room") String room, @Headers Map<String, Object> headers,
			@Payload String body, Principal principal, SimpMessageHeaderAccessor headerAccessor)
			throws IOException, InterruptedException {
		String topic = String.format("/topic/room/%s/join", room);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("room", room);
		headerAccessor.setSessionAttributes(attributes);
		System.out.println(String.format("user: %s -> room: %s -> body: %s", principal.getName(), room, body));
		Map<String, Object> map = new HashMap<>();
		map.put("session", headerAccessor.getSessionId());
		map.put("allSessions", getSessions(topic));

		messagingTemplate.convertAndSendToUser(principal.getName(), "/session", headerAccessor.getSessionId());
		messagingTemplate.convertAndSend(String.format("/topic/room/%s/join", room), map);

		// messagingTemplate.convertAndSendToUser(principal.getName(), "/message",
		// body);
	}


	@MessageMapping("/test/room/{room}/count")
	public void count(@DestinationVariable("room") String room, @Headers Map<String, Object> headers,
			@Payload String body, Principal principal) throws IOException, InterruptedException {
		String topic = String.format("/topic/room/%s", room);
		int count = getUsers(topic).size();
		System.out.println(String.format("user: %s -> room: %s -> body: %s", principal.getName(), room, body));
		messagingTemplate.convertAndSend(String.format("/topic/room/%s/count", room), count);

		// messagingTemplate.convertAndSendToUser(principal.getName(), "/message",
		// body);
	}

	@MessageMapping("/test/room/{room}/webrtc")
	public void webrtc(@DestinationVariable("room") String room, @Headers Map<String, Object> headers,
			@Payload String body, Principal principal) throws IOException, InterruptedException {
		System.out.println(String.format("webrtc: %s -> room: %s -> body: %s", principal.getName(), room, body));
		messagingTemplate.convertAndSend(String.format("/topic/room/%s/webrtc", room), body);
	}

	@MessageMapping("/test/room/{room}/user")
	public void user(@DestinationVariable("room") String room, @Headers Map<String, Object> headers,
			@Payload String body, Principal principal) throws IOException, InterruptedException {
		String topic = String.format("/topic/room/%s/user", room);
		List<String> users = getUsers(topic);
		System.out.println(String.format("get user: %s -> room: %s -> body: %s", principal.getName(), room, body));
		messagingTemplate.convertAndSend(String.format("/topic/room/%s/user", room), users);
	}

	@MessageMapping("/test/room/{room}/{type}")
	public void test(@DestinationVariable("room") String room, @DestinationVariable("type") String type,
			@Headers Map<String, Object> headers, @Payload String body, Principal principal)
			throws IOException, InterruptedException {
		System.out.println(
				String.format("user: %s -> room: %s -> type: %s -> body: %s", principal.getName(), room, type, body));
		messagingTemplate.convertAndSend(String.format("/topic/room/%s/%s", room, type), body);
	}

	@MessageMapping("/test/user/{user}/message")
	public void userMessage(@DestinationVariable("user") String user, @Headers Map<String, Object> headers,
			@Payload String body, Principal principal) throws IOException, InterruptedException {
		System.out
				.println(String.format("user message: %s -> to user: %s -> body: %s", principal.getName(), user, body));
		messagingTemplate.convertAndSendToUser(user, "/message", body);
	}

	@MessageMapping("/test/user/session")
	public void sessionId(@Headers Map<String, Object> headers, @Payload String body, Principal principal,
			SimpMessageHeaderAccessor headerAccessor) throws IOException, InterruptedException {
		System.out.println(String.format("session: %s -> to user: %s -> body: %s", headerAccessor.getSessionId(),
				principal.getName(), body));
		sendToSession(headerAccessor.getSessionId(), "/session",
				headerAccessor.getSessionId());
	}

	@MessageMapping("/test/user/webrtc")
	public void toSession(@Headers Map<String, Object> headers, @Payload String body, Principal principal,
			SimpMessageHeaderAccessor headerAccessor) throws IOException, InterruptedException {

		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};

		HashMap<String, Object> map = objectMapper.readValue(body, typeRef);
		String session = (String) map.get("session");
		String toSession = (String) map.get("toSession");

		System.out.println(String.format("user webrtc: %s -> to session: %s -> body: %s", session,
				toSession, body));

		map.put("session", headerAccessor.getSessionId());
		sendToSession(toSession, "/webrtc", objectMapper.writeValueAsString(map));
	}

	private void sendToSession(String sessionId, String destination, Object payload) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headerAccessor.setSessionId(sessionId);
		headerAccessor.setLeaveMutable(true);

		messagingTemplate.convertAndSendToUser(sessionId, destination, payload, headerAccessor.getMessageHeaders());
	}
}
