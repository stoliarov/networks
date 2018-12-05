package ru.nsu.stoliarov.task4.app.client.commands;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.JsonParser;
import ru.nsu.stoliarov.task4.app.Settings;
import ru.nsu.stoliarov.task4.app.client.CommandParams;
import ru.nsu.stoliarov.task4.app.client.HttpResponseParser;
import ru.nsu.stoliarov.task4.app.client.Result;

import java.io.IOException;
import java.util.Scanner;

public class Write extends Command {
	private DefaultHttpClient client;
	private String message = null;
	
	public Write() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		if(null == message) {
			return new Result(0, "/write", "Требуется ввод сообщения");
		}
		HttpPost postRequest = new HttpPost(Settings.serverURI + "/messages");
		try {
			StringEntity input = new StringEntity("{\"message\":\"" + message + "\"}", "UTF-8");
			input.setContentType("application/json");
			postRequest.addHeader("Authorization", "Token " + params.getToken());
			postRequest.setEntity(input);
			
			HttpResponse response = client.execute(postRequest);
			String body = HttpResponseParser.readBody(response);
			
			if(200 != response.getStatusLine().getStatusCode()) {
				return new Result(response.getStatusLine().getStatusCode(),getName(),
						"Возникла внутренняя ошибка: " + response.getStatusLine().getStatusCode());
			} else {
				return new Result(response.getStatusLine().getStatusCode(), getName(), "Отправлено");
			}
		} catch (IOException e) {
			return new Result(1, getName(), "Ошибка связи с сервером");
		}
	}
	
	@Override
	public void enterData() {
		System.out.println("Введите сообщение:");
		Scanner scanner = new Scanner(System.in);
		message = scanner.nextLine();
	}
	
	@Override
	public String getName() {
		return "/write";
	}
}
