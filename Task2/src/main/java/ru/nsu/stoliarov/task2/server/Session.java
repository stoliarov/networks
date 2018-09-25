package ru.nsu.stoliarov.task2.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task2.Connection;
import ru.nsu.stoliarov.task2.Event;
import ru.nsu.stoliarov.task2.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Session extends Thread {
	private static final Logger logger = LogManager.getLogger(Session.class.getName());
	
	private LinkedBlockingQueue<Task> tasks;
	Connection connection;
	
	private int fileSize;       // max 1 099 511 627 776 byte
	private String fileName;    // max 4096 byte
	
	public Session(LinkedBlockingQueue<Task> tasks, Socket socket) throws IOException {
		this.tasks = tasks;
		this.connection = new Connection(socket.getInputStream(), socket.getOutputStream(), socket);
	}
	
	@Override
	public void run() {
		try {
			getHi();
			getMetadata();
			getData();
			
		} catch (IOException e) {
			e.printStackTrace();
			connection.closeConnection();
		}
	}
	
	private void getHi() throws IOException {
		byte[] receivedBytes = readMessage();
		
		JSONObject receivedJson = JsonParser.getJSONbyBytes(receivedBytes, 0, receivedBytes.length);
		System.out.println("Server got:" + receivedJson.toString());
		
		if(null != receivedJson) {
			if(Event.HI.toString().equals(receivedJson.get("event"))) {
				JSONObject jsonToSend = new JSONObject();
				jsonToSend.put("event", Event.HI.toString());
				outStream.write(jsonToSend.toString().getBytes());
				outStream.flush();
			} else {
				throw new IOException("Received unexpected type of message: " + receivedJson.get("event")
						+ ". Expected: " + Event.HI.toString());
			}
		} else {
			throw new IOException("Fail to parse the received message: "
					+ new String(receivedBytes, 0, receivedBytes.length));
		}
	}
	
	private void getMetadata() {
	
	}
	
	private void getData() {
		while(true) {
		
		}
	}
}
