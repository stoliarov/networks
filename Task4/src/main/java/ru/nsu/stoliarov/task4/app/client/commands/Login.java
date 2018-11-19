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

public class Login extends Command {
	private DefaultHttpClient client;
	private String username = null;
	
	public Login() {
		client = new DefaultHttpClient();
	}
	
	@Override
	public Result execute(CommandParams params) {
		if(null == username) {
			return new Result(0, "/login", "Требуется ввод имени пользователя");
		}
		HttpPost postRequest = new HttpPost(Settings.serverURI + "/login");
		try {
			StringEntity input = new StringEntity("{\"username\":\"" + username + "\"}", "UTF-8");
			input.setContentType("application/json");
			postRequest.setEntity(input);
			
			HttpResponse response = client.execute(postRequest);
			
			String body = HttpResponseParser.readBody(response);
			
			if(401 == response.getStatusLine().getStatusCode()) {
				return new Result(401, "/login", "Выбранное имя уже занято");
			} else if(200 != response.getStatusLine().getStatusCode()) {
				return new Result(response.getStatusLine().getStatusCode(),"/login",
						"Возникла внутренняя ошибка: " + response.getStatusLine().getStatusCode());
			}
			
			JSONObject bodyJson = JsonParser.getJSONbyString(body);
			if(bodyJson != null) {
				return new Result(200, "/login", (String) bodyJson.get("historyKey"), (String) bodyJson.get("token"));
			} else {
				return new Result(0, "/login", "Не удалось прочитать json-ответ сервера");
			}
			
		} catch (IOException e) {
			return new Result(1, "/login", "Ошибка связи с сервером");
		}
	}
	
	@Override
	public void enterData() {
		System.out.println("Введите ваше имя для входа в чат:");
		Scanner scanner = new Scanner(System.in);
		username = scanner.nextLine();
	}
	
	@Override
	public String getName() {
		return "/login";
	}
}
