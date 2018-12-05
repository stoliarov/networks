package ru.nsu.stoliarov.task4.app.client;

public class CommandParams {
	private String token;
	private String data;
	private long offset = -1;
	private long count = -1;
	
	public CommandParams(String token) {
		this.token = token;
	}
	
	public CommandParams(String token, String data) {
		this.token = token;
		this.data = data;
	}
	
	public CommandParams(String token, long offset, long count) {
		this.token = token;
		this.offset = offset;
		this.count = count;
	}
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public boolean containsOffset() {
		return -1 != offset;
	}
	
	public boolean containsCount() {
		return -1 != count;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	public long getCount() {
		return count;
	}
	
	public void setCount(long count) {
		this.count = count;
	}
}
