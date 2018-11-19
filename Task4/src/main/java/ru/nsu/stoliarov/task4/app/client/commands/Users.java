package ru.nsu.stoliarov.task4.app.client.commands;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.JsonParser;
import ru.nsu.stoliarov.task4.app.Settings;
import ru.nsu.stoliarov.task4.app.client.CommandParams;
import ru.nsu.stoliarov.task4.app.client.HttpResponseParser;
import ru.nsu.stoliarov.task4.app.client.Result;

import java.io.IOException;

public class Users extends Command {
	private DefaultHttpClient client;
	
	public Users() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		HttpGet getRequest = new HttpGet(Settings.serverURI + "/users");
		try {
			getRequest.addHeader("Authorization", "Token " + params.getToken());
			HttpResponse response = client.execute(getRequest);
			String body = HttpResponseParser.readBody(response);
			
			if(403 == response.getStatusLine().getStatusCode()) {
				return new Result(403, getName(), "Требуется авторизация");
			} else if(200 != response.getStatusLine().getStatusCode()) {
				return new Result(response.getStatusLine().getStatusCode(),getName(),
						"Возникла внутренняя ошибка: " + response.getStatusLine().getStatusCode());
			} else {
				return new Result(response.getStatusLine().getStatusCode(), getName(), body);
			}
		} catch (IOException e) {
			return new Result(1, getName(), "Ошибка связи с сервером");
		}
	}
	
	@Override
	public String getName() {
		return "/users";
	}
}
