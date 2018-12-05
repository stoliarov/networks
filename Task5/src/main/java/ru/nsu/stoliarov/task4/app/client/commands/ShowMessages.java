package ru.nsu.stoliarov.task4.app.client.commands;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task4.app.JsonParser;
import ru.nsu.stoliarov.task4.app.Settings;
import ru.nsu.stoliarov.task4.app.client.CommandParams;
import ru.nsu.stoliarov.task4.app.client.HttpResponseParser;
import ru.nsu.stoliarov.task4.app.client.Result;

import java.io.IOException;

public class ShowMessages extends Command {
	private DefaultHttpClient client;
	
	public ShowMessages() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		StringBuilder uriBuilder = new StringBuilder();
		uriBuilder.append(Settings.serverURI + "/messages");
		if(params.containsOffset() && params.containsCount()) {
			uriBuilder.append("?offset=" + params.getOffset() + "&count=" + params.getCount());
		}
		
		HttpGet getRequest = new HttpGet(uriBuilder.toString());
		try {
			getRequest.addHeader("Authorization", "Token " + params.getToken());
			HttpResponse response = client.execute(getRequest);
			String body = HttpResponseParser.readBody(response);
			
			if(200 != response.getStatusLine().getStatusCode()) {
				return new Result(response.getStatusLine().getStatusCode(), getName(), body);
			}
			
			JSONObject bodyJson = JsonParser.getJSONbyString(body);
			if(null != bodyJson) {
				return new Result(200, getName(), body);
			} else {
				return new Result(0, getName(), "Не удалось прочитать json-ответ сервера");
			}
		} catch (IOException e) {
			return new Result(1, getName(), "Ошибка связи с сервером");
		}
	}
	
	@Override
	public String getName() {
		return "/showMessages";
	}
}
