package ru.nsu.stoliarov.task4.app.server.user;

import java.util.UUID;

public class User {
	private long id;
	private String username;
	private boolean online;
	private String token;
	private long confirmationTime;
	
	public User(String username, long id) {
		this.id = id;
		this.username = username;
		this.online = true;
		this.token = UUID.randomUUID().toString();
		this.confirmationTime = System.currentTimeMillis();
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public boolean isOnline() {
		return online;
	}
	
	public void setOnline(boolean online) {
		this.online = online;
	}
	
	public String getToken() {
		return token;
	}
	
	public void updateToken() {
		token = UUID.randomUUID().toString();
	}
	
	public long getConfirmationTime() {
		return confirmationTime;
	}
	
	public void setConfirmationTime(long confirmationTime) {
		this.confirmationTime = confirmationTime;
	}
	
	@Override
	public String toString() {
		return username + " (id = " + id + ", token = " + token + ")";
	}
}
