package ru.nsu.stoliarov.task4.app.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Client implements Runnable {
	private static Logger logger = LogManager.getLogger(Client.class.getName());
	
	private DefaultHttpClient client;
	private String uri;
	private String token;
	private long messagesCount;
	private Timer messagesUpdater;
	private boolean authenticateRequired = false;
	
	public Client(String uri) {
		this.client = new DefaultHttpClient();
		this.uri = uri;
		this.messagesCount = 0;
		this.messagesUpdater = new Timer();
	}
	
	// todo 1) комманды в executeCommand 2) таймер для отправки конфирмов 3) таймер для запросов инфы о смене активности пользователей
	
	@Override
	public void run() {
		while(200 != login()) {
		}
		waitForCommand();
	}
	
	private void waitForCommand() {
		Scanner scanner = new Scanner(System.in);
		printCommandsList();
		
		while(true) {
			String command = scanner.nextLine();
			executeCommand(command);
			if(authenticateRequired) {
				while(200 != login()) {}
			}
		}
	}
	
	private void executeCommand(String command) {
		if(command.length() > 5 && command.substring(0, 6).equals("/users")) {
		
		} else if(command.length() > 6 && command.substring(0, 5).equals("/user")) {
		
		} else if(command.length() > 4 && command.substring(0, 5).equals("/exit")) {
			int statusCode = logout();
			if(200 == statusCode) {
				messagesUpdater.cancel();
				while(200 != login()) {}
			} else if(403 == statusCode) {
				messagesUpdater.cancel();
				System.out.println("А у вас как раз токен просрочен. Так что вы успешно вышли");
				while(200 != login()) {}
			} else {
				System.out.println("Не удалось выполнить команду");
			}
			
		} else if(command.length() > 7 && command.substring(0, 6).equals("/write")) {
		
		} else if(command.length() > 4 && command.substring(0, 5).equals("/help")) {
			printCommandsList();
		} else {
			System.out.println("\"" + command + "\" - не является командой");
		}
	}
	
	private void printCommandsList() {
		System.out.println("------Список доступных комманд:------");
		System.out.println("/users - показать список активных пользователей");
		System.out.println("/user <id> - показать пользователя с указанным id");
		System.out.println("/exit - выйти");
		System.out.println("/write <some_message_text> - отправить сообщение");
		System.out.println("/help - показать список доступных комманд");
	}
	
	private int updateMessagesCount() {
		HttpGet getRequest = new HttpGet(uri + "/messages/size");
		try {
			getRequest.addHeader("Authorization", "Token " + token);
			HttpResponse response = client.execute(getRequest);
			String body = readBody(response);
			
			int statusCode = checkStatusCode(response);
			if(200 != statusCode) {
				return statusCode;
			}
			
			JSONObject bodyJson = JsonParser.getJSONbyString(body);
			messagesCount = (long) bodyJson.get("size");
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return 200;
	}
	
	private int showMessages(long offset, long count) {
		HttpGet getRequest = new HttpGet(uri + "/messages?offset=" + offset + "&count=" + count);
		return executeShowMessages(getRequest);
	}
	
	private int showMessages(long offset) {
		HttpGet getRequest = new HttpGet(uri + "/messages?offset=" + offset);
		return executeShowMessages(getRequest);
	}
	
	private int executeShowMessages(HttpGet getRequest) {
		try {
			getRequest.addHeader("Authorization", "Token " + token);
			HttpResponse response = client.execute(getRequest);
			String body = readBody(response);
			
			int statusCode = checkStatusCode(response);
			if(200 != statusCode) {
				return statusCode;
			}
			
			JSONObject bodyJson = JsonParser.getJSONbyString(body);
			JSONArray messagesJson = (JSONArray) bodyJson.get("messages");
			messagesJson.forEach(v -> {
				JSONObject message = (JSONObject) v;
				printMessage((String) message.get("message"), (String) message.get("author"));
			});
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return 200;
	}
	
	
	private void showUsers() {
	
	}
	
	private void printMessage(String message, String author) {
		System.out.println("<Получено сообщение \"" + message + "\" от " + author + ">");
	}
	
	private int login() {
		System.out.println("Введите ваше имя для входа в чат:");
		Scanner scanner = new Scanner(System.in);
		String username = scanner.nextLine();
		
		HttpPost postRequest = new HttpPost(uri + "/login");
		try {
			StringEntity input = new StringEntity("{\"username\":\"" + username + "\"}", "UTF-8");
			input.setContentType("application/json");
			postRequest.addHeader("Authorization", "Token ");
			postRequest.setEntity(input);
			
			HttpResponse response = client.execute(postRequest);
			
			String body = readBody(response);
			
			if(401 == response.getStatusLine().getStatusCode()) {
				System.out.println("Выбранное имя уже занято");
				return 401;
			} else if(200 != response.getStatusLine().getStatusCode()) {
				return innerError(response);
			}
			
			JSONObject bodyJson = JsonParser.getJSONbyString(body);
			token = (String) bodyJson.get("token");
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Авторизация прошла успешно");
		messagesUpdater = new Timer();
		messagesUpdater.schedule(new ShowNewMessages(), 500, 500);
		
		return 200;
	}
	
	private int logout() {
		HttpPost postRequest = new HttpPost(uri + "/logout");
		try {
			postRequest.addHeader("Authorization", "Token " + token);
			HttpResponse response = client.execute(postRequest);
			BufferedReader reader = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
			String body = readBody(response);
			
			int statusCode = checkStatusCode(response);
			if(200 != statusCode) {
				return statusCode;
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Вы успешно вышли из сети");
		
		return 200;
	}
	
	private String readBody(HttpResponse response) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
		String output;
		StringBuilder builder = new StringBuilder();
		while((output = reader.readLine()) != null) {
			builder.append(output);
		}
		logger.debug("Получено от сервера: " + builder.toString() + " "
				+ response.getStatusLine().getStatusCode() + " " + response.getAllHeaders());
		
		return builder.toString();
	}
	
	private int checkStatusCode(HttpResponse response) {
		if(403 == response.getStatusLine().getStatusCode()) {
			System.out.println("Требуется повторная аутентификация");
			return 403;
		} else if(200 != response.getStatusLine().getStatusCode()) {
			return innerError(response);
		}
		return response.getStatusLine().getStatusCode();
	}
	
	private int innerError(HttpResponse response) {
		System.out.println("Возникла внутренняя ошибка: " + response.getStatusLine().getStatusCode());
		return response.getStatusLine().getStatusCode();
	}
	
	private class ShowNewMessages extends TimerTask {
		@Override
		public void run() {
			long oldMessagesCount = messagesCount;
			int statusCode = updateMessagesCount();
			if(200 != statusCode) {
				messagesUpdater.cancel();
				authenticateRequired = true;
				System.out.println("Не удалось получить messagesCount у сервера. Вывод новых сообщений приостановлен");
			} else if(oldMessagesCount < messagesCount){
				if(200 != showMessages(oldMessagesCount, messagesCount)) {
					authenticateRequired = true;
					messagesUpdater.cancel();
					System.out.println("Не удалось получить новые сообщения. Вывод сообщений приостановлен");
				}
			}
		}
	}
}
