package ru.nsu.stoliarov.task4.app.client;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.JsonParser;
import ru.nsu.stoliarov.task4.app.Settings;
import ru.nsu.stoliarov.task4.app.client.commands.Command;
import ru.nsu.stoliarov.task4.app.server.Server;

import javax.websocket.*;
import javax.websocket.server.ServerEndpointConfig;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Client implements Runnable {
	private static Logger logger = LogManager.getLogger(Client.class.getName());
	
	private String token = "";
	private String historyKey = "";
	private long messagesCount;
	private Timer messagesUpdater;
	private Timer confirmSender;
	private Timer activityUpdater;
	private Session session;
	
	private LinkedBlockingQueue<Task> tasks;
	private LinkedBlockingQueue<Result> userResults;
	private ConcurrentHashMap<String, Result> systemResults;
	private Thread taskExecutor;
	
	public Client() {
		this.session = null;
		this.messagesCount = 0;
		this.messagesUpdater = new Timer();
		this.confirmSender = new Timer();
		this.activityUpdater = new Timer();
		this.tasks = new LinkedBlockingQueue<>(10000);
		this.userResults = new LinkedBlockingQueue<>(10000);
		this.systemResults = new ConcurrentHashMap<>();
		this.taskExecutor = new Thread(new TaskExecutor(tasks, userResults, systemResults));
	}
	
	@Override
	public void run() {
		taskExecutor.start();
		try {
			Scanner scanner = new Scanner(System.in);
			printCommandsList();

			while(true) {
				String commandName = scanner.nextLine();
				executeCommand(commandName);
			}
		} catch (InterruptedException e) {
			logger.debug("Client is interrupted");
		}
	}
	
	private void executeCommand(String commandName) throws InterruptedException {
		try {
			Command command = Factory.getInstance().getCommand(commandName);
			command.enterData();
			
			Task task = new Task(command, new CommandParams(token), true);
			if(!tasks.offer(task)) {
				logger.warn("Failed to execute the command. The tasks queue is busy.");
			}
			
			Result result = userResults.take();
			if(1 == result.getStatusCode()) {
				System.out.println(result.getData());
				resend(task);
				result = userResults.take();
				if(1 == result.getStatusCode()) {
					System.out.println(result.getData());
					resend(task);
					result = userResults.take();
					if(1 == result.getStatusCode()) {
						System.out.println("Сервер недоступен");
						printCommandsList();
						return;
					}
				}
			}
			
			if(200 == result.getStatusCode() && result.getTaskName().equals("/login")) {
				token = result.getToken();
				historyKey = result.getData();
				System.out.println("Вы успешно авторизовались");
				startSession();
				return;
			} else if(200 == result.getStatusCode() && result.getTaskName().equals("/logout")) {
				System.out.println(result.getData());
				endSession();
				return;
			}
			
			if(result.containsData()) {
				System.out.println(result.getData());
			}
			
		} catch (DatatypeConfigurationException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			System.out.println("Такой команды не существует");
		}
	}
	
	private void resend(Task task) throws InterruptedException {
		Thread.sleep(2000);
		if(!tasks.offer(task)) {
			logger.warn("Failed to execute the command. The tasks queue is busy.");
		}
	}
	
	private void printCommandsList() {
		System.out.println("------Список доступных комманд:------");
		System.out.println("/login - войти в систему");
		System.out.println("/logout - выйти из системы");
		System.out.println("/users - показать список активных пользователей");
		System.out.println("/user - показать пользователя с указанным id");
		System.out.println("/write - отправить сообщение");
	}
	
	private void endSession() {
		messagesUpdater.cancel();
		confirmSender.cancel();
		activityUpdater.cancel();
		
		try {
			session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		messagesUpdater = new Timer();
//		activityUpdater = new Timer();
		confirmSender = new Timer();
	}
	
	private void startSession() throws InterruptedException {
		long oldMessagesCount = messagesCount;
		if(updateMessageCount() && oldMessagesCount < messagesCount) {
			showMessages(oldMessagesCount, messagesCount - oldMessagesCount);
		}
		
		try {
			ClientManager client = ClientManager.createClient();
			session = client.connectToServer
					(ClientPoint.class, new URI("ws://localhost:8088/ws/polling"));
			session.getBasicRemote().sendText(token);
			
		} catch (DeploymentException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}

//		messagesUpdater.schedule(new ShowNewMessages(), 500, 500);
//		activityUpdater.schedule(new ShowUserActivities(), 500, 500);
		confirmSender.schedule(new ConfirmationSending(), Settings.CONFIRMATION_TIMEOUT, Settings.CONFIRMATION_TIMEOUT);
	}
	
	private void printMessage(String message, String author) {
		System.out.println("<" + author + " написал: " + message + ">");
	}
	
	private void sendConfirm() throws InterruptedException {
		try {
			Command command = Factory.getInstance().getCommand("/confirm");
			
			Task task = new Task(command, new CommandParams(token), false);
			if(!tasks.offer(task)) {
				logger.warn("Failed to execute the command. The tasks queue is busy.");
			}
			
			while(!systemResults.containsKey("/confirm")) {
				Thread.sleep(100);
			}
			Result result = systemResults.get("/confirm");
			systemResults.remove("/confirm");
			
		} catch (DatatypeConfigurationException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			logger.error("System command not found");
			e.printStackTrace();
		}
	}
	
	private void showMessages(long offset, long count) throws InterruptedException {
		try {
			Command command = Factory.getInstance().getCommand("/showMessages");

			Task task = new Task(command, new CommandParams(token, offset, count), false);
			if(!tasks.offer(task)) {
				logger.warn("Failed to execute the command. The tasks queue is busy.");
			}

			while(!systemResults.containsKey("/showMessages")) {
				Thread.sleep(100);
			}
			Result result = systemResults.get("/showMessages");
			systemResults.remove("/showMessages");

			if(200 != result.getStatusCode()) {
				logger.error("Failed to get new messages");
			} else {
				JSONObject bodyJson = JsonParser.getJSONbyString(result.getData());
				JSONArray messagesJson = (JSONArray) bodyJson.get("messages");
				messagesJson.forEach(v -> {
					JSONObject message = (JSONObject) v;
					printMessage((String) message.get("message"), (String) message.get("author"));
				});
			}
		} catch (DatatypeConfigurationException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			logger.error("System command not found");
			e.printStackTrace();
		}
	}
	
	private void showActivities() throws InterruptedException {
		try {
			Command command = Factory.getInstance().getCommand("/showActivities");
			
			Task task = new Task(command, new CommandParams(token, historyKey), false);
			if(!tasks.offer(task)) {
				logger.warn("Failed to execute the command. The tasks queue is busy.");
			}
			
			while(!systemResults.containsKey("/showActivities")) {
				Thread.sleep(100);
			}
			Result result = systemResults.get("/showActivities");
			systemResults.remove("/showActivities");
			
			if(200 != result.getStatusCode()) {
				logger.error("Failed to get new activities");
			} else {
				JSONObject bodyJson = JsonParser.getJSONbyString(result.getData());
				historyKey = (String) bodyJson.get("historyKey");
				JSONArray activitiesJson = (JSONArray) bodyJson.get("activities");
				activitiesJson.forEach(v -> {
					JSONObject message = (JSONObject) v;
					System.out.println("<" + (String) message.get("activity") + ">");
				});
			}
		} catch (DatatypeConfigurationException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			logger.error("System command not found");
			e.printStackTrace();
		}
	}
	
	private boolean updateMessageCount() throws InterruptedException {
		try {
			Command command = Factory.getInstance().getCommand("/messagesCount");
			
			Task task = new Task(command, new CommandParams(token), false);
			if(!tasks.offer(task)) {
				logger.warn("Failed to execute the command. The tasks queue is busy.");
			}
			
			while(!systemResults.containsKey("/messagesCount")) {
				Thread.sleep(100);
			}
			Result result = systemResults.get("/messagesCount");
			systemResults.remove("/messagesCount");
			
			if(200 != result.getStatusCode()) {
				logger.error("Failed to update messages count");
				return false;
			} else {
				messagesCount = Long.parseLong(result.getData());
				return true;
			}
		} catch (IllegalAccessException | InstantiationException | DatatypeConfigurationException | ClassNotFoundException e) {
			logger.error("System command not found");
			return false;
		}
	}
	
	private class ShowUserActivities extends TimerTask {
		@Override
		public void run() {
			try {
				showActivities();
			} catch (InterruptedException e) {
				logger.debug("ShowUserActivities timer is interrupted");
			}
		}
	}
	
	private class ShowNewMessages extends TimerTask {
		@Override
		public void run() {
			long oldMessagesCount = messagesCount;
			try {
				if(updateMessageCount() && oldMessagesCount < messagesCount) {
					showMessages(oldMessagesCount, messagesCount - oldMessagesCount);
				}
			} catch (InterruptedException e) {
				logger.debug("ShowNewMessages timer task is interrupted");
			}
		}
	}
	
	private class ConfirmationSending extends TimerTask {
		@Override
		public void run() {
			try {
				sendConfirm();
			} catch (InterruptedException e) {
				logger.debug("Confirmation sending is interrupted");
			}
		}
	}
}
