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
import ru.nsu.stoliarov.task4.app.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class Client implements Runnable {
	private static Logger logger = LogManager.getLogger(Client.class.getName());
	
	private DefaultHttpClient client;
	private String uri;
	private String token;
	private long messagesCount;
	private Timer messagesUpdater;
	private Timer confirmSender;
	private boolean authenticateRequired = false;
	
	private LinkedBlockingQueue<Task> toSend;
	private LinkedBlockingQueue<Result> results;
	private Thread sender;
	
	public Client(String uri) {
		this.client = new DefaultHttpClient();
		this.uri = uri;
		this.messagesCount = 0;
		this.messagesUpdater = new Timer();
		this.confirmSender = new Timer();
		this.toSend = new LinkedBlockingQueue<>(10000);
		this.results = new LinkedBlockingQueue<>(10000);
		this.sender = new Thread(new Sender(uri, toSend, results));
	}
	
	// todo 1) комманды в executeCommand 2) таймер для отправки конфирмов 3) таймер для запросов инфы о смене активности пользователей
	
	@Override
	public void run() {
		sender.start();
		
//		while(200 != login()) {
//		}
//		waitForCommand();
	}
//
//	private void waitForCommand() {
//		Scanner scanner = new Scanner(System.in);
//		printCommandsList();
//
//		while(true) {
//			String command = scanner.nextLine();
//			executeCommand(command);
//			if(authenticateRequired) {
//				while(200 != login()) {}
//			}
//		}
//	}
//
//	private void executeCommand(String command) {
//		if(command.length() > 5 && command.substring(0, 6).equals("/users")) {
//
//		} else if(command.length() > 6 && command.substring(0, 5).equals("/user")) {
//
//		} else if(command.length() > 4 && command.substring(0, 5).equals("/exit")) {
//			int statusCode = logout();
//			if(200 == statusCode) {
//				endSession();
//				while(200 != login()) {}
//
//			} else if(403 == statusCode) {
//				endSession();
//				System.out.println("А у вас как раз токен просрочен. Так что вы успешно вышли");
//				while(200 != login()) {}
//
//			} else {
//				System.out.println("Не удалось выполнить команду");
//			}
//
//		} else if(command.length() > 7 && command.substring(0, 6).equals("/write")) {
//
//		} else if(command.length() > 4 && command.substring(0, 5).equals("/help")) {
//			printCommandsList();
//		} else {
//			System.out.println("\"" + command + "\" - не является командой");
//		}
//	}
//
//	private void printCommandsList() {
//		System.out.println("------Список доступных комманд:------");
//		System.out.println("/users - показать список активных пользователей");
//		System.out.println("/user <id> - показать пользователя с указанным id");
//		System.out.println("/exit - выйти");
//		System.out.println("/write <some_message_text> - отправить сообщение");
//		System.out.println("/help - показать список доступных комманд");
//	}
//
//	private void endSession() {
//		messagesUpdater.cancel();
//		confirmSender.cancel();
//		messagesUpdater = new Timer();
//		confirmSender = new Timer();
//		authenticateRequired = true;
//	}
//
//	private void startSession() {
//		authenticateRequired = false;
//		messagesUpdater.schedule(new ShowNewMessages(), 500, 500);
//		confirmSender.schedule(new ConfirmationSending(), Settings.CONFIRMATION_TIMEOUT, Settings.CONFIRMATION_TIMEOUT);
//	}
//
//	private int updateMessagesCount() {
//		HttpGet getRequest = new HttpGet(uri + "/messages/size");
//		try {
//			getRequest.addHeader("Authorization", "Token " + token);
//			HttpResponse response = client.execute(getRequest);
//			String body = readBody(response);
//
//			int statusCode = checkStatusCode(response);
//			if(200 != statusCode) {
//				return statusCode;
//			}
//
//			JSONObject bodyJson = JsonParser.getJSONbyString(body);
//			messagesCount = (long) bodyJson.get("size");
//
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return 200;
//	}
//
//	private int showMessages(long offset, long count) {
//		HttpGet getRequest = new HttpGet(uri + "/messages?offset=" + offset + "&count=" + count);
//		return executeShowMessages(getRequest);
//	}
//
//	private int showMessages(long offset) {
//		HttpGet getRequest = new HttpGet(uri + "/messages?offset=" + offset);
//		return executeShowMessages(getRequest);
//	}
//
//	private int executeShowMessages(HttpGet getRequest) {
//		try {
//			getRequest.addHeader("Authorization", "Token " + token);
//			HttpResponse response = client.execute(getRequest);
//			String body = readBody(response);
//
//			int statusCode = checkStatusCode(response);
//			if(200 != statusCode) {
//				return statusCode;
//			}
//
//			JSONObject bodyJson = JsonParser.getJSONbyString(body);
//			JSONArray messagesJson = (JSONArray) bodyJson.get("messages");
//			messagesJson.forEach(v -> {
//				JSONObject message = (JSONObject) v;
//				printMessage((String) message.get("message"), (String) message.get("author"));
//			});
//
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return 200;
//	}
//
//
//	private void showUsers() {
//
//	}
//
//	private void printMessage(String message, String author) {
//		System.out.println("<Получено сообщение \"" + message + "\" от " + author + ">");
//	}
//
//	private int login() throws InterruptedException {
//		System.out.println("Введите ваше имя для входа в чат:");
//		Scanner scanner = new Scanner(System.in);
//		String username = scanner.nextLine();
//
//		toSend.offer(new Task(TaskName.LOGIN.toString(), username));
//		Result result = results.take();
//
//		if(200 == result.getStatusCode()) {
//			startSession();
//		}
//
//		return result.getStatusCode();
//	}
//
//	private int logout() {
//		HttpPost postRequest = new HttpPost(uri + "/logout");
//		try {
//			postRequest.addHeader("Authorization", "Token " + token);
//			HttpResponse response = client.execute(postRequest);
//			String body = readBody(response);
//
//			int statusCode = checkStatusCode(response);
//			if(200 != statusCode) {
//				return statusCode;
//			}
//
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		System.out.println("Вы успешно вышли из сети");
//
//		return 200;
//	}
//
//	private int sendConfirm() {
//		HttpPost postRequest = new HttpPost(uri + "/confirm");
//		try {
//			StringEntity input = new StringEntity("{\"confirm\":\" \"}", "UTF-8");
//			input.setContentType("application/json");
//			postRequest.addHeader("Authorization", "Token " + token);
//			postRequest.setEntity(input);
//
//			HttpResponse response = client.execute(postRequest);
//
//			String body = readBody(response);
//
//			int statusCode = checkStatusCode(response);
//			if(200 != statusCode) {
//				return statusCode;
//			}
//
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return 200;
//	}
//
//	private class ShowNewMessages extends TimerTask {
//		@Override
//		public void run() {
//			long oldMessagesCount = messagesCount;
//			int statusCode = updateMessagesCount();
//			if(200 != statusCode) {
//				endSession();
//				System.out.println("Не удалось получить messagesCount у сервера. Вывод новых сообщений приостановлен");
//			} else if(oldMessagesCount < messagesCount){
//				if(200 != showMessages(oldMessagesCount, messagesCount)) {
//					endSession();
//					System.out.println("Не удалось получить новые сообщения. Вывод сообщений приостановлен");
//				}
//			}
//		}
//	}
//
//	private class ConfirmationSending extends TimerTask {
//		@Override
//		public void run() {
////			int statusCode = sendConfirm();
////			if(200 != statusCode) {
////				endSession();
////			}
//		}
//	}
}
