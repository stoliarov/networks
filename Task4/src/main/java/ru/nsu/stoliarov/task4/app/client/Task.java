package ru.nsu.stoliarov.task4.app.client;

public class Task {
	private String taskName;
	private String data;
	private long offset;
	private long count;
	
	public Task(String taskName) {
		this.taskName = taskName;
	}
	
	public Task(String taskName,String data) {
		this.taskName = taskName;
		this.data = data;
	}
	
	public Task(String taskName, long offset) {
		this.taskName = taskName;
		this.offset = offset;
	}
	
	public Task(String taskName, long offset, long count) {
		this.taskName = taskName;
		this.offset = offset;
		this.count = count;
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
}
