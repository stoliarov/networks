package ru.nsu.stoliarov.task4.app.server.events;

import javax.websocket.Session;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class EventService {
	static private LinkedBlockingQueue<Event> events = new LinkedBlockingQueue<>();
	static private ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
	static public ConcurrentHashMap<String, String> ids = new ConcurrentHashMap<>();
	
	private EventService() {
	}
	
	public static void addSession(Session session) {
		sessions.put(session.getId(), session);
	}
	
	public static void addSession(Session session, String token) {
		sessions.put(session.getId(), session);
		setTokenForSessionId(token, session.getId());
	}
	
	public static void removeSession(String id) {
		sessions.remove(id);
	}
	
	public static void setTokenForSessionId(String token, String id) {
		ids.put(token, id);
	}
	
	public static void addEvent(String description) {
		events.add(new Event(description));
	}
	
	public static void addEvent(String description, String ignoredToken) {
		events.add(new Event(description, ignoredToken));
	}
	
	public static BlockingQueue<Event> getEvents() {
		return events;
	}
	
	public static ConcurrentHashMap<String, Session> getSessions() {
		return sessions;
	}
	
	public static String getSessionIdByToken(String token) {
		return ids.get(token);
	}
}
