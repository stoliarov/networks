package ru.nsu.stoliarov.task4.app.client.commands;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import ru.nsu.stoliarov.task4.app.Settings;
import ru.nsu.stoliarov.task4.app.client.CommandParams;
import ru.nsu.stoliarov.task4.app.client.HttpResponseParser;
import ru.nsu.stoliarov.task4.app.client.Result;

import java.io.IOException;

public class Confirm extends Command {
	private DefaultHttpClient client;
	private String username = null;
	
	public Confirm() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		HttpPost postRequest = new HttpPost(Settings.serverURI + "/confirm");
		try {
			StringEntity input = new StringEntity("{\"confirm\":\" \"}", "UTF-8");
			input.setContentType("application/json");
			postRequest.addHeader("Authorization", "Token " + params.getToken());
			postRequest.setEntity(input);
			
			HttpResponse response = client.execute(postRequest);
			String body = HttpResponseParser.readBody(response);
			
			return new Result(response.getStatusLine().getStatusCode(), getName(), body);
			
		} catch (IOException e) {
			return new Result(1, getName(), "Ошибка связи с сервером");
		}
	}
	
	@Override
	public String getName() {
		return "/confirm";
	}
}
