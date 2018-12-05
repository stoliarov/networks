package ru.nsu.stoliarov.task4.app.server;

import ru.nsu.stoliarov.task4.app.server.events.EventService;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint (value = "/polling")
public class ServerPoint {
	private Set<ServerPoint> chatEndpoints = new CopyOnWriteArraySet<>();
	
	@OnOpen
	public void onOpen(Session session) {
	}
	
	@OnMessage
	public void onMessage(String message, Session session) {
		EventService.addSession(session, message);
	}
	
	@OnClose
	public void onClose(Session session) {
		EventService.removeSession(session.getId());
	}
}
