package ru.nsu.stoliarov.task4.app.server.events;

public class Event {
	private String description;
	private String ignoredToken;
	
	public Event(String description) {
		this.description = description;
		this.ignoredToken = null;
	}
	
	public Event(String description, String ignoredToken) {
		this.description = description;
		this.ignoredToken = ignoredToken;
	}
	
	public boolean hasIgnoredToken() {
		return null != ignoredToken;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getIgnoredToken() {
		return ignoredToken;
	}
	
	public void setIgnoredToken(String ignoredToken) {
		this.ignoredToken = ignoredToken;
	}
}
