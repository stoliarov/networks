package ru.nsu.stoliarov.task2;

import org.json.simple.JSONObject;

public class Message {
	private JSONObject head;
	private byte[] data;
	
	public Message(JSONObject head, byte[] data) {
		this.head = head;
		this.data = data;
	}
	
	public JSONObject getHead() {
		return head;
	}
	
	public void setHead(JSONObject head) {
		this.head = head;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
}
