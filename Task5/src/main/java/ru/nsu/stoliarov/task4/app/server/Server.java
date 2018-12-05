package ru.nsu.stoliarov.task4.app.server;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.*;
import ru.nsu.stoliarov.task4.app.server.events.EventSender;
import ru.nsu.stoliarov.task4.app.server.events.EventService;
import ru.nsu.stoliarov.task4.app.server.message.Message;
import ru.nsu.stoliarov.task4.app.server.message.MessageService;
import ru.nsu.stoliarov.task4.app.server.user.User;
import ru.nsu.stoliarov.task4.app.server.user.UserActivityHistory;
import ru.nsu.stoliarov.task4.app.server.user.UserService;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Server implements Runnable {
	private final int DEFAULT_OFFSET = 0;
	private final int DEFAULT_COUNT = 3;
	private final int MAX_COUNT = 100;
	
	private Logger logger = LogManager.getLogger(Server.class.getName());
	Map<String, Map<String, MappingCommand>> mapping;
	private Undertow undertow;
	private boolean comparatorFlag;
	private UserService userService;
	private MessageService messageService;
	private UserActivityHistory userActivityHistory;
	private Thread eventSender;
//	private ObjectMapper mapper;
	
	public Server() {
//		mapper = new ObjectMapper();
		this.eventSender = new Thread(new EventSender());
		userService = new UserService();
		messageService = new MessageService();
		userActivityHistory = new UserActivityHistory();
		mapping = new HashMap<>();
		fillMapping();
		
		Timer usersListUpdater = new Timer();
		usersListUpdater.schedule(new ListOfUsersUpdating(), Settings.CONFIRMATION_TIMEOUT, Settings.CONFIRMATION_TIMEOUT);
	}
	
	
	
	@Override
	public void run() {
		eventSender.start();
		
		org.glassfish.tyrus.server.Server server =
				new org.glassfish.tyrus.server.Server
						("localhost", 8088, "/ws", ServerPoint.class);
		
		try {
			server.start();
		} catch (DeploymentException e) {
			server.stop();
			throw new RuntimeException(e);
		}
		
		undertow = Undertow.builder()
				.addHttpListener(Settings.serverPort, Settings.serverHost)
				.setServerOption(UndertowOptions.IDLE_TIMEOUT, 100000)
				.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 100000)
				.setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, 100000)
				.setServerOption(UndertowOptions.URL_CHARSET, "UTF-8")
				.setServerOption(UndertowOptions.DECODE_URL, false)
				.setHandler(new BlockingHandler(exchange -> {
					String path = URIParser.extractPathBeforeNumbers(exchange.getRequestPath());
					if(mapping.containsKey(path)) {
						if(mapping.get(path).containsKey(exchange.getRequestMethod().toString())) {
							mapping.get(path).get(exchange.getRequestMethod().toString()).execute(exchange);
						} else {
							exchange.setStatusCode(405);
							exchange.getResponseSender().send("Method Not Allowed");
						}
					} else {
						System.out.println(404);
						exchange.setStatusCode(404);
						exchange.getResponseSender().send("Page not found");
					}
				})).build();

		undertow.start();
	}
	
	private void fillMapping() {
		mapping.put("/login", new HashMap<>());
		mapping.get("/login").put("POST", new LoginMappingPost());
		
		mapping.put("/logout", new HashMap<>());
		mapping.get("/logout").put("POST", new LogoutMappingPost());
		
		mapping.put("/users", new HashMap<>());
		mapping.get("/users").put("GET", new UsersMappingGet());
		
		mapping.put("/messages", new HashMap<>());
		mapping.get("/messages").put("GET", new MessagesMappingGet());
		mapping.get("/messages").put("POST", new MessagesMappingPost());
		
		mapping.put("/messages/size", new HashMap<>());
		mapping.get("/messages/size").put("GET", new MessagesSizeMappingGet());
		
		mapping.put("/confirm", new HashMap<>());
		mapping.get("/confirm").put("POST", new ConfirmMappingPost());
		
		mapping.put("/users", new HashMap<>());
		mapping.get("/users").put("GET", new UsersMappingGet());
		
		mapping.put("/activity", new HashMap<>());
		mapping.get("/activity").put("GET", new ActivityMappingGet());
	}
	
	private void authorize(HttpServerExchange exchange, String username) {
		if(userService.containsUsername(username)) {
			if(userService.getUserByName(username).isOnline()) {
				exchange.setStatusCode(401);
				exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, "Token realm='Username is already in use'");
				return;
			} else {
				User user = userService.getUserByName(username);
				userService.login(user);
				user.setConfirmationTime(System.currentTimeMillis());
			}
		} else {
			userService.addUser(username);
			userService.getUserByName(username).setConfirmationTime(System.currentTimeMillis());
			userService.login(userService.getUserByName(username));
		}
		
		JSONObject responseJson = new JSONObject();
		responseJson.put("id", userService.getUserByName(username).getId());
		responseJson.put("username", username);
		responseJson.put("online", userService.getUserByName(username).isOnline());
		responseJson.put("token", userService.getUserByName(username).getToken());
		userActivityHistory.add("Пользователь " + username + " зашел в сеть");
		responseJson.put("historyKey", userActivityHistory.lastKey());
		
		EventService.addEvent("<Пользователь " + username + " зашел в сеть>",
				userService.getUserByName(username).getToken());
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private void sendUserInfo(HttpServerExchange exchange, long requiredId) {
		JSONObject responseJson = new JSONObject();
		responseJson.put("id", requiredId);
		responseJson.put("username", userService.getUserById(requiredId).getUsername());
		responseJson.put("online", userService.getUserById(requiredId).isOnline());
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private void sendAllUsersInfo(HttpServerExchange exchange) {
		JSONObject responseJson = new JSONObject();
		JSONArray usersJson = new JSONArray();
		userService.getActiveUsers().forEach(entry -> {
			JSONObject userJson = new JSONObject();
			userJson.put("id", entry.getValue().getId());
			userJson.put("username", entry.getValue().getUsername());
			userJson.put("online", entry.getValue().isOnline());
			usersJson.add(userJson);
		});
		responseJson.put("users", usersJson);
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private void sendMessages(HttpServerExchange exchange, int offset, int count) {
		JSONObject responseJson = new JSONObject();
		JSONArray messagesJson = new JSONArray();
		
		messageService.getMessages(offset, count).forEach(message -> {
			JSONObject messageJson = new JSONObject();
			messageJson.put("id", message.getId());
			messageJson.put("message", message.getText());
			messageJson.put("author", message.getAuthorName());
			messagesJson.add(messageJson);
		});
		
		responseJson.put("messages", messagesJson);
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private String readBody(final HttpServerExchange exchange) {
		BufferedReader reader = null;
		StringBuilder builder = new StringBuilder();
		
		try {
			exchange.startBlocking();
			reader = new BufferedReader(new InputStreamReader(exchange.getInputStream()));
			
			String line;
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return builder.toString();
	}
	
	private String getTokenByExchange(final HttpServerExchange exchange) {
		return exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString()).toString().substring(7, 43);
	}
	
	private boolean checkToken(final HttpServerExchange exchange) {
		if(!exchange.getRequestHeaders().contains("Authorization")
				|| exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString()).toString().length() < 43
				|| !exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString()).toString().substring(0, 6).equals("[Token")) {
			
			
			exchange.setStatusCode(401);
			exchange.getResponseSender().send("Unauthorized");
			return false;
			
		} else if(!userService.containsToken(exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString())
				.toString().substring(7, 43))) {
			
			logger.debug("Unexpected token: " + exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString())
					.toString().substring(7, 43));
			
			exchange.setStatusCode(403);
			exchange.getResponseSender().send("Forbidden");
			return false;
			
		} else {
			return true;
		}
	}
	
	public class CheckToken implements MappingCommand {
		private final MappingCommand delegate;
		
		public CheckToken(MappingCommand delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public void execute(HttpServerExchange exchange) {
			if(!exchange.getRequestHeaders().contains("Authorization")
					|| exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString()).toString().length() < 43
					|| !exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString()).toString().substring(0, 6).equals("[Token")) {
				
				
				exchange.setStatusCode(401);
				exchange.getResponseSender().send("Unauthorized");
				return;
				
			} else if(!userService.containsToken(exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString())
					.toString().substring(7, 43))) {

//				logger.debug("Unexpected token: " + exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString())
//						.toString().substring(7, 43));
				
				exchange.setStatusCode(403);
				exchange.getResponseSender().send("Forbidden");
				return;
				
			} else {
				delegate.execute(exchange);
			}
		}
	}
	
	private boolean isExpectedHeaders(final HttpServerExchange exchange, Map<String, String> expectedHeaders) {
		comparatorFlag = false;
		expectedHeaders.forEach((k, v) -> {
			if(!exchange.getRequestHeaders().contains(k) || !exchange.getRequestHeaders().get(k).toString().equals("[" + v + "]")) {
				comparatorFlag = true;
			}
		});
		
		if(comparatorFlag) {
			logger.debug("Unexpected headers: " + exchange.getRequestHeaders() + ". Expected: " + expectedHeaders);
			return false;
		} else {
			return true;
		}
	}
	
	private boolean isExpectedMethod(final HttpServerExchange exchange, String expectedMethod) {
		if(!expectedMethod.equals(exchange.getRequestMethod().toString())) {
			logger.debug("Unexpected request method: " + exchange.getRequestMethod() + ". Expected: " + expectedMethod);
			return false;
		} else {
			return true;
		}
	}
	
	private class LoginMappingPost implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json");
			if(!isExpectedHeaders(exchange, headers)) {
				exchange.setStatusCode(400);
				exchange.getResponseSender().send("Bad Request");
				return;
			}
			
			JSONObject bodyJson = JsonParser.getJSONbyString(readBody(exchange));
			if(null == bodyJson || !bodyJson.containsKey("username") || "".equals((String) bodyJson.get("username"))) {
				exchange.setStatusCode(400);
				exchange.getResponseSender().send("Bad Request");
				return;
			}
			
			String username = (String) bodyJson.get("username");
			authorize(exchange, username);
		}
	}
	
	private class LogoutMappingPost implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			if(checkToken(exchange)) {
				User user = userService.getUserByToken(getTokenByExchange(exchange));
				
				EventService.addEvent("<Пользователь " + user.getUsername() + " вышел из сети>", user.getToken());
				
				userService.logout(user);
				
				JSONObject responseJson = new JSONObject();
				responseJson.put("message", "bye!");
				
				userActivityHistory.add("Пользователь " + user.getUsername() + " вышел из сети");
				
				exchange.setStatusCode(200);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(responseJson.toString());
			}
		}
	}
	
	private class UsersMappingGet implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			List<String> pathItems = URIParser.pathItems(exchange.getRequestPath());
			
			if(checkToken(exchange)) {
				if(1 == pathItems.size()) {
					sendAllUsersInfo(exchange);
					
				} else if(2 == pathItems.size()) {
					if(URIParser.isNumeric(pathItems.get(1))) {
						long requiredId = URIParser.longValue(pathItems.get(1));
						if(userService.containsId(requiredId)) {
							sendUserInfo(exchange, requiredId);
							
						} else {
							exchange.setStatusCode(404);
							exchange.getResponseSender().send("Page not found");
						}
					} else {
						exchange.setStatusCode(404);
						exchange.getResponseSender().send("Page not found");
					}
				} else {
					exchange.setStatusCode(404);
					exchange.getResponseSender().send("Page not found");
				}
			}
		}
	}
	
	private class MessagesMappingGet implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			if(checkToken(exchange)) {
				int offset = DEFAULT_OFFSET;
				int count = DEFAULT_COUNT;
				
				if(exchange.getQueryParameters().containsKey("offset")) {
					String offsetString = exchange.getQueryParameters().get("offset").toString();
					if(offsetString.length() > 2) {
						offsetString = offsetString.substring(1, offsetString.length() - 1);
						try {
							offset = Integer.parseInt(offsetString);
						} catch (IndexOutOfBoundsException e) {
							// юзаем значение по умолчанию
						}
					}
				}
				if(exchange.getQueryParameters().containsKey("count")) {
					String countString = exchange.getQueryParameters().get("count").toString();
					if(countString.length() > 2) {
						countString = countString.substring(1, countString.length() - 1);
						try {
							count = Integer.parseInt(countString);
						} catch (IndexOutOfBoundsException e) {
							// юзаем значение по умолчанию
						}
					}
				}
				
				if(offset < 0) {
					offset = DEFAULT_OFFSET;
				}
				if(count < 0) {
					count = DEFAULT_COUNT;
				}
				if(count > MAX_COUNT) {
					count = MAX_COUNT;
				}
				
				sendMessages(exchange, offset, count);
			}
		}
	}
	
	private class MessagesMappingPost implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json");
			if(!isExpectedHeaders(exchange, headers)) {
				exchange.setStatusCode(400);
				exchange.getResponseSender().send("Bad Request");
				return;
			}
			
			if(checkToken(exchange)) {
				JSONObject bodyJson = JsonParser.getJSONbyString(readBody(exchange));
				if(null == bodyJson || !bodyJson.containsKey("message") || "".equals((String) bodyJson.get("message"))) {
					exchange.setStatusCode(400);
					exchange.getResponseSender().send("Bad Request");
				} else {
					Message message = messageService.addMessage(bodyJson.get("message").toString(),
							userService.getUserByToken(getTokenByExchange(exchange)).getUsername());
					
					JSONObject responseJson = new JSONObject();
					responseJson.put("id", message.getId());
					responseJson.put("message", message.getText());
					
					EventService.addEvent("<" + message.getAuthorName() + " написал: " + message.getText() + ">");
					
					exchange.setStatusCode(200);
					exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
					exchange.getResponseSender().send(responseJson.toString());
				}
			}
		}
	}
	
	private class MessagesSizeMappingGet implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			if(checkToken(exchange)) {
				JSONObject responseJson = new JSONObject();
				responseJson.put("size", messageService.messagesCount());
				
				exchange.setStatusCode(200);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(responseJson.toString());
			}
		}
	}
	
	private class ConfirmMappingPost implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json");
			if(!isExpectedHeaders(exchange, headers)) {
				exchange.setStatusCode(400);
				exchange.getResponseSender().send("Bad Request");
				return;
			}
			
			if(checkToken(exchange)) {
				JSONObject bodyJson = JsonParser.getJSONbyString(readBody(exchange));
				if(null == bodyJson || !bodyJson.containsKey("confirm")) {
					exchange.setStatusCode(400);
					exchange.getResponseSender().send("Bad Request");
				} else {
					userService.getUserByToken(getTokenByExchange(exchange)).setConfirmationTime(System.currentTimeMillis());
					exchange.setStatusCode(200);
				}
			}
		}
	}
	
	@ServerEndpoint (value = "/messages1")
	private class MessagesEndpoint {
		@OnOpen
		public void onOpen(Session session) {
			System.out.println("open");
			try {
				session.getBasicRemote().sendText("Hello");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@OnMessage
		public void onMessage(Session session, String message) {
			System.out.println("message");
			System.out.println(message);
		}
	}
	
	private class ActivityMappingGet implements MappingCommand {
		@Override
		public void execute(HttpServerExchange exchange) {
			if(checkToken(exchange)) {
				JSONObject responseJson = new JSONObject();
				JSONArray activitiesJson = new JSONArray();
				String key = exchange.getQueryParameters().get("key").toString();
				if(key.length() > 37) {
					key = key.substring(1, 37);
				}
				List<String> activities = userActivityHistory.subHistory(key);
				
				activities.forEach(activity -> {
					JSONObject activityJson = new JSONObject();
					activityJson.put("activity", activity);
					activitiesJson.add(activityJson);
				});
				
				responseJson.put("activities", activitiesJson);
				responseJson.put("historyKey", userActivityHistory.lastKey());
				
				exchange.setStatusCode(200);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(responseJson.toString());
			}
		}
	}
	
	private class ListOfUsersUpdating extends TimerTask {
		@Override
		public void run() {
			userService.getActiveUsers().forEach(entry -> {
				if(System.currentTimeMillis() - entry.getValue().getConfirmationTime() > Settings.CONFIRMATION_TIMEOUT * 3) {
					User user = userService.getUserByToken(entry.getValue().getToken());
					userService.logout(user);
					userActivityHistory.add("Пользователь " + user.getUsername() + " выпал по таймауту");
					EventService.addEvent("<Пользователь " + user.getUsername() + " выпал по таймауту>");
				}
			});
		}
	}
}
