package ru.nsu.stoliarov.task2.server;

public class Task {
	private String fileName;
	private boolean isLast;
	private byte[] data;
	
	public Task(String fileName, byte[] data, boolean isLast) {
		this.fileName = fileName;
		this.data = data;
		this.isLast = isLast;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public boolean isLast() {
		return isLast;
	}
	
	public void setLast(boolean last) {
		isLast = last;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
}
