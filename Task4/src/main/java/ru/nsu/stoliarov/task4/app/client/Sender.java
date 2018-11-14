package ru.nsu.stoliarov.task4.app.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.LinkedBlockingQueue;

public class Sender implements Runnable {
	private static final Logger logger = LogManager.getLogger(Sender.class.getName());
	
	private DefaultHttpClient client;
	private String uri;
	private String token;
	private LinkedBlockingQueue<Task> toSend;
	private LinkedBlockingQueue<Result> results;
	
	public Sender(String  uri, LinkedBlockingQueue<Task> toSend, LinkedBlockingQueue<Result> results) {
		this.uri = uri;
		this.toSend = toSend;
		this.results = results;
		this.client = new DefaultHttpClient();
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Task task = toSend.take();
				
				if(task.getTaskName().equals(TaskName.LOGIN.toString())) {
					login(task);
				}
				// todo
				
			} catch (InterruptedException e) {
				logger.info("Sender is interrupted");
				return;
			}
		}
	}
	
	private int login(Task task) {
		HttpPost postRequest = new HttpPost(uri + "/login");
		try {
			StringEntity input = new StringEntity("{\"username\":\"" + task.getData() + "\"}", "UTF-8");
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
}
