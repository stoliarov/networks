package ru.nsu.stoliarov.task4.app.server;

public class Message {
	private long id;
	private String text;
	private String authorName;
	
	public Message() {
	}
	
	public Message(long id, String text, String authorName) {
		this.id = id;
		this.text = text;
		this.authorName = authorName;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getAuthorName() {
		return authorName;
	}
	
	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}
}
