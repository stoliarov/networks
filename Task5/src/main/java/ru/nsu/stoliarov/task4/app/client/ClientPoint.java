package ru.nsu.stoliarov.task4.app.client;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

@ClientEndpoint
public class ClientPoint {
	@OnMessage
	public void onMessage(String message) {
		System.out.println(message);
	}
}
