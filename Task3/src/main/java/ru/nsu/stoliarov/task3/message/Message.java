package ru.nsu.stoliarov.task3.message;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.net.InetAddress;

public class Message {
	private static final Logger logger = LogManager.getLogger(Message.class.getName());
	
	private Event event;
	private String text;
	private String senderName;
	private String guid;
	private InetAddress address;
	private int port;
	private long time;
	private boolean isSent = false;
	
	public Message(JSONObject jsonObject) {
	
	}
	
	public Message(Event event, String guid, InetAddress address, int port) {
		this.event = event;
		this.text = null;
		this.senderName = null;
		this.guid = guid;
		this.address = address;
		this.port = port;
	}
	
	public Message(Event event, String text, String senderName, String guid, InetAddress address, int port) {
		this.event = event;
		this.text = text;
		this.senderName = senderName;
		this.guid = guid;
		this.address = address;
		this.port = port;
	}
	
	public boolean containSenderName() {
		return null != this.senderName;
	}
	
	public boolean containText() {
		return null != this.text;
	}
	
	public static String getIdByAddressAndGUID(InetAddress address, int port, String guid) {
		return guid + address.getHostAddress() + port;
	}
	
	public String getId() {
		return getIdByAddressAndGUID(address, port, guid);
	}
	
	public Event getEvent() {
		return event;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getSenderName() {
		return senderName;
	}
	
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public void setGuid(String guid) {
		this.guid = guid;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public long getTime() {
		return time;
	}
	
	public void sent(long sendingTime) {
		this.time = sendingTime;
		isSent = true;
	}
	
	public boolean isSent() {
		return isSent;
	}
	
	@Override
	public boolean equals(Object obj) {
		try {
			Message message = (Message) obj;
			return message.getId().equals(this.getId());
		} catch (ClassCastException e) {
			logger.warn("Try to compare object of type Message with object of other type!");
			return false;
		}
	}
}
