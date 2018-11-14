package ru.nsu.stoliarov.task4.app.server;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server {
	private static final int DEFAULT_OFFSET = 0;
	private static final int DEFAULT_COUNT = 3;
	private static final int MAX_COUNT = 100;
	
	private static Logger logger = LogManager.getLogger(Server.class.getName());
	private static boolean comparatorFlag;
	private static Map<String, User> users;
	private static ConcurrentMap<String, User> activeUsers;
	private static Map<String, User> actualTokens;
	private static Map<Long, User> actualIDs;
	private static Map<Long, Message> messages;
	private static Timer usersListUpdater;
	
	private Server() {}
	
	public static Undertow createServer() {
		users = new HashMap<>();
		activeUsers = new ConcurrentHashMap<>();
		actualTokens = new HashMap<>();
		actualIDs = new HashMap<>();
		messages = new HashMap<>();
		
		// todo фабрика комманд для парсинга path либо паттерн command (в фабрике)
		// todo сделать UserService чтобы в сервере не дергать мапы
		// todo сделать TokenService
		// todo почитай про jackson, мб будешь парсить String с json в объект класса
		
		usersListUpdater = new Timer();
		usersListUpdater.schedule(new ListOfUsersUpdating(), Settings.CONFIRMATION_TIMEOUT, Settings.CONFIRMATION_TIMEOUT);
		
		return Undertow.builder()
				.addHttpListener(8080, "localhost")
				.setServerOption(UndertowOptions.IDLE_TIMEOUT, 100000)
				.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 100000)
				.setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, 100000)
				.setServerOption(UndertowOptions.URL_CHARSET, "UTF-8")
				.setServerOption(UndertowOptions.DECODE_URL, false)
				.setHandler(new BlockingHandler(exchange -> {
					if(exchange.getRequestPath().equals("/login")) {
						loginMapping(exchange);
						
					} else if(exchange.getRequestPath().equals("/logout")) {
						logoutMapping(exchange);
						
					} else if(exchange.getRequestPath().equals("/messages")) {
						messagesMapping(exchange);
						
					} else if(exchange.getRequestPath().equals("/messages/size")) {
						messagesSizeMapping(exchange);
						
					} else if(exchange.getRequestPath().equals("/confirm")) {
						confirmMapping(exchange);
					
					} else {
						List<String> pathItems = URIParser.pathItems(exchange.getRequestPath());
						if(null != pathItems && pathItems.size() > 0 && pathItems.get(0).equals("/users")) {
							usersMapping(exchange, pathItems);
						} else {
							exchange.setStatusCode(404);
							exchange.getResponseSender().send("Page not found");
						}
					}
					
				})).build();
	}
	
	private static void loginMapping(final HttpServerExchange exchange) {
		if(!isExpectedMethod(exchange, "POST")) {
			exchange.setStatusCode(405);
			exchange.getResponseSender().send("Method Not Allowed");
			return;
		}
		
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		if(!isExpectedHeaders(exchange, headers)) {
			exchange.setStatusCode(400);
			exchange.getResponseSender().send("Bad Request");
			return;
		}
		
		
		JSONObject bodyJson = JsonParser.getJSONbyString(readBody(exchange));
		if(!bodyJson.containsKey("username") || "".equals((String) bodyJson.get("username"))) {
			exchange.setStatusCode(400);
			exchange.getResponseSender().send("Bad Request");
			return;
		}
		
		String username = (String) bodyJson.get("username");
		authorize(exchange, username);
	}
	
	private static void authorize(HttpServerExchange exchange, String username) {
		if(users.containsKey(username)) {
			if(users.get(username).isOnline()) {
				exchange.setStatusCode(401);
				exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, "Token realm='Username is already in use'");
				return;
			} else {
				User user = users.get(username);
				user.setOnline(true);
				user.setConfirmationTime(System.currentTimeMillis());
				activeUsers.put(username, user);
			}
		} else {
			User user = new User(username, users.size());
			user.setConfirmationTime(System.currentTimeMillis());
			users.put(username, user);
			activeUsers.put(username, user);
			actualTokens.put(user.getToken(), user);
			actualIDs.put(user.getId(), user);
		}
		
		JSONObject responseJson = new JSONObject();
		responseJson.put("id", users.get(username).getId());
		responseJson.put("username", username);
		responseJson.put("online", users.get(username).isOnline());
		responseJson.put("token", users.get(username).getToken());
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private static void logoutMapping(final HttpServerExchange exchange) {
		if(!isExpectedMethod(exchange, "POST")) {
			exchange.setStatusCode(405);
			exchange.getResponseSender().send("Method Not Allowed");
			return;
		}
		
		if(checkToken(exchange)) {
			logout(getTokenByExchange(exchange));
			
			JSONObject responseJson = new JSONObject();
			responseJson.put("message", "bye!");
			
			exchange.setStatusCode(200);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
			exchange.getResponseSender().send(responseJson.toString());
		}
	}
	
	private static void usersMapping(final HttpServerExchange exchange, List<String> pathItems) {
		if(!isExpectedMethod(exchange, "GET")) {
			exchange.setStatusCode(405);
			exchange.getResponseSender().send("Method Not Allowed");
			return;
		}
		
		if(checkToken(exchange)) {
			if(1 == pathItems.size()) {
				sendAllUsersInfo(exchange);
				
			} else if(2 == pathItems.size()) {
				if(URIParser.isNumeric(pathItems.get(1))) {
					long requiredId = URIParser.longValue(pathItems.get(1));
					if(actualIDs.containsKey(requiredId)) {
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
	
	private static void sendUserInfo(HttpServerExchange exchange, long requiredId) {
		JSONObject responseJson = new JSONObject();
		responseJson.put("id", requiredId);
		responseJson.put("username", actualIDs.get(requiredId).getUsername());
		responseJson.put("online", actualIDs.get(requiredId).isOnline());
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private static void sendAllUsersInfo(HttpServerExchange exchange) {
		JSONObject responseJson = new JSONObject();
		JSONArray usersJson = new JSONArray();
		activeUsers.forEach((k, v) -> {
			JSONObject userJson = new JSONObject();
			userJson.put("id", v.getId());
			userJson.put("username", v.getUsername());
			userJson.put("online", v.isOnline());
			usersJson.add(userJson);
		});
		responseJson.put("users", usersJson);
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private static void messagesMapping(final HttpServerExchange exchange) {
		if(exchange.getRequestMethod().toString().equals("POST")) {
			receiveMessage(exchange);
		} else if(exchange.getRequestMethod().toString().equals("GET")) {
			if(checkToken(exchange)) {
				showMessages(exchange);
			}
		} else {
			exchange.setStatusCode(405);
			exchange.getResponseSender().send("Method Not Allowed");
		}
	}
	
	private static void showMessages(HttpServerExchange exchange) {
		long offset = DEFAULT_OFFSET;
		long count = DEFAULT_COUNT;
		
		if(exchange.getQueryParameters().containsKey("offset")) {
			String offsetString = exchange.getQueryParameters().get("offset").toString();
			if(offsetString.length() > 2) {
				offsetString = offsetString.substring(1, offsetString.length() - 1);
				try {
					offset = Long.parseLong(offsetString);
				} catch (IndexOutOfBoundsException e) {
					// то юзаем значение по умолчанию
				}
			}
		}
		if(exchange.getQueryParameters().containsKey("count")) {
			String countString = exchange.getQueryParameters().get("count").toString();
			if(countString.length() > 2) {
				countString = countString.substring(1, countString.length() - 1);
				try {
					count = Long.parseLong(countString);
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
	
	private static void sendMessages(HttpServerExchange exchange, long offset, long count) {
		JSONObject responseJson = new JSONObject();
		JSONArray messagesJson = new JSONArray();
		
		for(long i = offset; i < messages.size(); ++i) {
			JSONObject messageJson = new JSONObject();
			messageJson.put("id", messages.get(i).getId());
			messageJson.put("message", messages.get(i).getText());
			messageJson.put("author", messages.get(i).getAuthorName());
			messagesJson.add(messageJson);
			
			--count;
			if(count <= 0) {
				break;
			}
		}
		
		responseJson.put("messages", messagesJson);
		
		exchange.setStatusCode(200);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(responseJson.toString());
	}
	
	private static void receiveMessage(HttpServerExchange exchange) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		if(!isExpectedHeaders(exchange, headers)) {
			exchange.setStatusCode(400);
			exchange.getResponseSender().send("Bad Request");
			return;
		}
		
		if(checkToken(exchange)) {
			JSONObject bodyJson = JsonParser.getJSONbyString(readBody(exchange));
			if(!bodyJson.containsKey("message") || "".equals((String) bodyJson.get("message"))) {
				exchange.setStatusCode(400);
				exchange.getResponseSender().send("Bad Request");
			} else {
				Message message = new Message(messages.size(),
						bodyJson.get("message").toString(),
						actualTokens.get(getTokenByExchange(exchange)).getUsername()
				);
				messages.put((long) messages.size(), message);
				
				JSONObject responseJson = new JSONObject();
				responseJson.put("id", message.getId());
				responseJson.put("message", message.getText());
				
				exchange.setStatusCode(200);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(responseJson.toString());
			}
		}
	}
	
	private static void messagesSizeMapping(HttpServerExchange exchange) {
		if(!isExpectedMethod(exchange, "GET")) {
			exchange.setStatusCode(405);
			exchange.getResponseSender().send("Method Not Allowed");
			return;
		}
		
		if(checkToken(exchange)) {
			JSONObject responseJson = new JSONObject();
			responseJson.put("size", messages.size());
			
			exchange.setStatusCode(200);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
			exchange.getResponseSender().send(responseJson.toString());
		}
	}
	
	private static void confirmMapping(HttpServerExchange exchange) {
		if(!isExpectedMethod(exchange, "POST")) {
			exchange.setStatusCode(405);
			exchange.getResponseSender().send("Method Not Allowed");
			return;
		}
		
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		if(!isExpectedHeaders(exchange, headers)) {
			exchange.setStatusCode(400);
			exchange.getResponseSender().send("Bad Request");
			return;
		}
		
		if(checkToken(exchange)) {
			JSONObject bodyJson = JsonParser.getJSONbyString(readBody(exchange));
			if(!bodyJson.containsKey("confirm")) {
				exchange.setStatusCode(400);
				exchange.getResponseSender().send("Bad Request");
			} else {
				actualTokens.get(getTokenByExchange(exchange)).setConfirmationTime(System.currentTimeMillis());
				exchange.setStatusCode(200);
			}
		}
	}
	
	private static void logout(String token) {
		actualTokens.get(token).setOnline(false);
		activeUsers.remove(actualTokens.get(token).getUsername());
		updateToken(actualTokens.get(token));
	}
	
	private static String readBody(final HttpServerExchange exchange) {
		BufferedReader reader = null;
		StringBuilder builder = new StringBuilder();
		
		try {
			exchange.startBlocking();
			reader = new BufferedReader(new InputStreamReader(exchange.getInputStream()));
			
			String line;
			while((line = reader.readLine()) != null ) {
				builder.append(line);
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return builder.toString( );
	}
	
	private static String getTokenByExchange(final HttpServerExchange exchange) {
		return exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString()).toString().substring(7, 43);
	}
	
	private static void updateToken(User user) {
		logger.debug("Previous token of user " + user.getUsername() + ": " + user.getToken());
		actualTokens.remove(user.getToken());
		user.updateToken();
		actualTokens.put(user.getToken(), user);
		logger.debug("New token of user " + user.getUsername() + ": " + user.getToken());
	}
	
	private static boolean checkToken(final HttpServerExchange exchange) {
		if(!exchange.getRequestHeaders().contains("Authorization")
				|| !exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString()).toString().substring(0, 6).equals("[Token")) {
			
			
			exchange.setStatusCode(401);
			exchange.getResponseSender().send("Unauthorized");
			return false;
			
		} else if(!actualTokens.containsKey(exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString())
				.toString().substring(7, 43))) {
			
			logger.debug("Unexpected token: " + exchange.getRequestHeaders().get(Headers.AUTHORIZATION.toString())
					.toString().substring(7, 43) + " Expected: " + actualTokens);
			
			exchange.setStatusCode(403);
			exchange.getResponseSender().send("Forbidden");
			return false;
			
		} else {
			return true;
		}
	}
	
	private static boolean isExpectedHeaders(final HttpServerExchange exchange, Map<String, String> expectedHeaders) {
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
	
	private static boolean isExpectedMethod(final HttpServerExchange exchange, String expectedMethod) {
		if(!expectedMethod.equals(exchange.getRequestMethod().toString())) {
			logger.debug("Unexpected request method: " + exchange.getRequestMethod() + ". Expected: " + expectedMethod);
			return false;
		} else {
			return true;
		}
	}
	
	private static class ListOfUsersUpdating extends TimerTask {
		@Override
		public void run() {
			activeUsers.forEach((k, v) -> {
				if(System.currentTimeMillis() - v.getConfirmationTime() > Settings.CONFIRMATION_TIMEOUT * 3) {
					logout(v.getToken());
					// todo выпавшие по таймауту пусть имеют онлайн null
				}
			});
		}
	}
}
