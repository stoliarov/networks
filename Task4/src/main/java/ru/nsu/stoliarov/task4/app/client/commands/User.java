package ru.nsu.stoliarov.task4.app.client.commands;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import ru.nsu.stoliarov.task4.app.Settings;
import ru.nsu.stoliarov.task4.app.client.CommandParams;
import ru.nsu.stoliarov.task4.app.client.HttpResponseParser;
import ru.nsu.stoliarov.task4.app.client.Result;

import java.io.IOException;
import java.util.Scanner;

public class User extends Command {
	private DefaultHttpClient client;
	private int id;
	
	public User() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		HttpGet getRequest = new HttpGet(Settings.serverURI + "/users/" + id);
		try {
			getRequest.addHeader("Authorization", "Token " + params.getToken());
			HttpResponse response = client.execute(getRequest);
			String body = HttpResponseParser.readBody(response);
			
			if(403 == response.getStatusLine().getStatusCode()) {
				return new Result(403, getName(), "Требуется авторизация");
			} else if(404 == response.getStatusLine().getStatusCode()) {
				return new Result(404, getName(), "Пользователь с таким id не найден");
			} else if(200 != response.getStatusLine().getStatusCode()) {
				return new Result(response.getStatusLine().getStatusCode(), getName(),
						"Возникла внутренняя ошибка: " + response.getStatusLine().getStatusCode());
			} else {
				return new Result(response.getStatusLine().getStatusCode(), getName(), body);
			}
		} catch (IOException e) {
			return new Result(1, getName(), "Ошибка связи с сервером");
		}
		
	}
	
	@Override
	public void enterData() {
		System.out.println("Введите id пользователя:");
		Scanner scanner = new Scanner(System.in);
		id = scanner.nextInt();
	}
	
	@Override
	public String getName() {
		return "/user";
	}
}
