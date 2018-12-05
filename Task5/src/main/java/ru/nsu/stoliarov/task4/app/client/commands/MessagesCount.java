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

public class MessagesCount extends Command {
	private DefaultHttpClient client;
	
	public MessagesCount() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		HttpGet getRequest = new HttpGet(Settings.serverURI + "/messages/size");
		try {
			getRequest.addHeader("Authorization", "Token " + params.getToken());
			HttpResponse response = client.execute(getRequest);
			String body = HttpResponseParser.readBody(response);
			
			if(200 != response.getStatusLine().getStatusCode()) {
				return new Result(response.getStatusLine().getStatusCode(), getName(), body);
			}
			
			JSONObject bodyJson = JsonParser.getJSONbyString(body);
			if(null != bodyJson) {
				return new Result(200, getName(), String.valueOf(bodyJson.get("size")));
			} else {
				return new Result(0, getName(), "Не удалось прочитать json-ответ сервера");
			}
			
		} catch (IOException e) {
			return new Result(1, getName(), "Ошибка связи с сервером");
		}
	}
	
	@Override
	public String getName() {
		return "/messagesCount";
	}
}
