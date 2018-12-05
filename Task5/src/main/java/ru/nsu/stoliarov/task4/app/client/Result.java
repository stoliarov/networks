package ru.nsu.stoliarov.task4.app.client;

public class Result {
	private int statusCode;
	private String taskName;
	private String data = null;
	private String token = null;
	
	public Result(int statusCode, String taskName) {
		this.statusCode = statusCode;
		this.taskName = taskName;
	}
	
	public Result(int statusCode, String taskName, String data) {
		this.statusCode = statusCode;
		this.taskName = taskName;
		this.data = data;
	}
	
	public Result(int statusCode, String taskName, String data, String token) {
		this.statusCode = statusCode;
		this.taskName = taskName;
		this.data = data;
		this.token = token;
	}
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public String getTaskName() {
		return taskName;
	}
	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public boolean containsData() {
		return null != data;
	}
}
