package com.forsrc.webrtc.config;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.sun.security.auth.UserPrincipal;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Autowired
	private WebSocketChannelInterceptor webSocketChannelInterceptor;

	@Autowired
	private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/message", "/session", "/webrtc", "/user");
		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/websocket").addInterceptors(webSocketHandshakeInterceptor).withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {

		registration.interceptors(webSocketChannelInterceptor);
	}

	@Component
	public static class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

		@Override
		public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

			if (request instanceof ServletServerHttpRequest) {
				ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
				HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
				HttpSession session = servletRequest.getServletRequest().getSession();
				String username = httpServletRequest.getParameter("username");
				String password = httpServletRequest.getParameter("password");
				System.out.println(String.format("-SessionHandshakeInterceptor-> %s -> %s/%s",
						session == null ? null : session.getId(), username, password));
				if (username != null) {
					attributes.put("username", username);
				}
				if (session != null) {
					attributes.put("sessionId", session.getId());
				}
			}

			return true;
		}

		@Override
		public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
				Exception exception) {

		}

	}

	@Component
	public static class WebSocketChannelInterceptor implements ChannelInterceptor {

		private static final ConcurrentHashMap<String, String> MAP = new ConcurrentHashMap();
		
		@Autowired
		private SimpMessageSendingOperations messagingTemplate;

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			String sessionId = accessor.getSessionId();
			if (StompCommand.CONNECT.equals(accessor.getCommand())) {

				accessor.getSessionAttributes().put("sessionId", sessionId);

				String username = accessor.getFirstNativeHeader("username");
				String password = accessor.getFirstNativeHeader("password");
				System.out.println(String.format("-WebSocketChannelInterceptor-> CONNECT %s -> %s/%s", sessionId,
						username, password));
				if (!StringUtils.isEmpty(username)) {
					accessor.getSessionAttributes().put("username", username);
					Principal principal = new UserPrincipal(username);
					accessor.setUser(principal);
					accessor.setNativeHeader("password", "***");
					MAP.put(username, sessionId);
				}
			}
			if (StompCommand.CONNECTED.equals(accessor.getCommand())) {
//				final StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(accessor.getCommand());
//				headerAccessor.setSessionId(sessionId);
//                @SuppressWarnings("unchecked")
//                final MultiValueMap<String, String> nativeHeaders = (MultiValueMap<String, String>) accessor.getHeader(StompHeaderAccessor.NATIVE_HEADERS);
//                headerAccessor.addNativeHeaders(nativeHeaders);
//
//                headerAccessor.setHeader("sessionId", sessionId);
//				Map<String, Object> map = new HashMap<>();
//				map.put("sessionId", sessionId);
//				accessor.addNativeHeader("sessionId", sessionId);
//				final Message<?> msg = MessageBuilder.createMessage(sessionId, headerAccessor.getMessageHeaders());
//				return msg;
			}
			if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
				String username = accessor.getUser().getName();
				System.out.println(
						String.format("-WebSocketChannelInterceptor-> DISCONNECT %s -> %s", sessionId, username));
				if (!StringUtils.isEmpty(username)) {
					MAP.remove(username);
				}
				messagingTemplate.convertAndSend("/topic/leave", sessionId);

			}

			return message;
		}
	}
}