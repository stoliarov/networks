package ru.nsu.stoliarov.task2.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task2.Connection;
import ru.nsu.stoliarov.task2.Event;
import ru.nsu.stoliarov.task2.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Session extends Thread {
	private static final Logger logger = LogManager.getLogger(Session.class.getName());
	
	private LinkedBlockingQueue<Task> tasks;
	Connection connection;
	
	private long fileSize;      // max 1 099 511 627 776 byte
	private String fileName;    // max 4096 byte
	
	public Session(LinkedBlockingQueue<Task> tasks, Socket socket) throws IOException {
		this.tasks = tasks;
		this.connection = new Connection(socket.getInputStream(), socket.getOutputStream(), socket);
	}
	
	@Override
	public void run() {
		try {
			receiveHi();
			receiveMetadata();
			receiveData();
			sendResult(true);
			connection.closeConnection();
			
		} catch (IOException e) {
			e.printStackTrace();
			sendResult(false);
			connection.closeConnection();
		}
	}
	
	private void receiveHi() throws IOException {
		connection.receiveMessage(Event.HI);
		
		JSONObject head = new JSONObject();
		head.put("event", Event.HI.toString());
		connection.sendMessage(new Message(head, null));
	}
	
	private void receiveMetadata() throws IOException {
		JSONObject metadata = connection.receiveMessage(Event.METADATA).getHead();
		fileName = (String) metadata.get("name");
		fileSize = (long) metadata.get("size");
		
		JSONObject head = new JSONObject();
		head.put("event", Event.GOT.toString());
		head.put("number", 0);
		connection.sendMessage(new Message(head, null));
	}
	
	private void receiveData() throws IOException {
		long quantityOfDataPiece = fileSize / connection.DATA_SIZE + 1;
		
		for(long i = 0; i < quantityOfDataPiece; ++i) {
			Message message = connection.receiveMessage(Event.DATA);
			
			if(i + 1 != (long) message.getHead().get("number")) {
				throw new IOException("Got unexpected number of data piece");
			}
			
			if(null == message.getData()) {
				throw new IOException("Got empty data");
			}
			
			if(!tasks.offer(new Task(fileName, message.getData(), i + 1 == quantityOfDataPiece))) {
				throw new IOException("Queue is overloaded");
			}
			
			JSONObject head = new JSONObject();
			head.put("event", Event.GOT.toString());
			head.put("number", message.getHead().get("number"));
			connection.sendMessage(new Message(head, null));
		}
	}
	
	private void sendResult(boolean isSuccess) {
		try {
			connection.receiveMessage(Event.GET_RESULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JSONObject head = new JSONObject();
		head.put("event", Event.RESULT.toString());
		head.put("status", isSuccess);
		
		try {
			connection.sendMessage(new Message(head, null));
		} catch (IOException e) {
			logger.warn("Sending of result failed");
			e.printStackTrace();
		}
	}
}
