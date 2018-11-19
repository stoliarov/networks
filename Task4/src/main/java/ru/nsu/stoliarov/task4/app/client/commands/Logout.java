package ru.nsu.stoliarov.task4.app.client.commands;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import ru.nsu.stoliarov.task4.app.Settings;
import ru.nsu.stoliarov.task4.app.client.CommandParams;
import ru.nsu.stoliarov.task4.app.client.HttpResponseParser;
import ru.nsu.stoliarov.task4.app.client.Result;

import java.io.IOException;

public class Logout extends Command {
	private DefaultHttpClient client;
	
	public Logout() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		HttpPost postRequest = new HttpPost(Settings.serverURI + "/logout");
		try {
			postRequest.addHeader("Authorization", "Token " + params.getToken());
			HttpResponse response = client.execute(postRequest);
			String body = HttpResponseParser.readBody(response);
			
			if(403 == response.getStatusLine().getStatusCode()) {
				return new Result(403, "/logout", "Требуется авторизация");
			} else if(200 != response.getStatusLine().getStatusCode()) {
				return new Result(response.getStatusLine().getStatusCode(),getName(),
						"Возникла внутренняя ошибка: " + response.getStatusLine().getStatusCode());
			} else {
				return new Result(200, "/logout", "Успешно");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return new Result(0, "/logout", "Ошибка связи с сервером");
		}
	}
	
	@Override
	public String getName() {
		return "/logout";
	}
}
