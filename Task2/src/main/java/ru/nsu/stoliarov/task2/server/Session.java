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
	private Connection connection;
	private Thread speedMeasurer;
	
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
			speedMeasurer.interrupt();
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
		fileSize = Long.valueOf(metadata.get("size").toString());
		
		JSONObject head = new JSONObject();
		head.put("event", Event.GOT.toString());
		head.put("number", 0);
		connection.sendMessage(new Message(head, null));
	}
	
	private void receiveData() throws IOException {
		long quantityOfDataPiece = fileSize / connection.DATA_SIZE + 1;
		
		LinkedBlockingQueue<Long> messageLengths = new LinkedBlockingQueue<Long>(100000);
		speedMeasurer = new SpeedMeasurer(messageLengths, System.currentTimeMillis());
		speedMeasurer.start();
		
		for(long i = 0; i < quantityOfDataPiece; ++i) {
			Message message = connection.receiveMessage(Event.DATA);
			messageLengths.offer(message.length());
			
			if(i + 1 != Long.valueOf(message.getHead().get("number").toString())) {
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
		
		speedMeasurer.interrupt();
	}
	
	private void sendResult(boolean isSuccess) {
		Message received = null;
		
		if(isSuccess) {
			try {
				received = connection.receiveMessage(Event.GET_RESULT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		JSONObject head = new JSONObject();
		head.put("event", Event.RESULT.toString());
		head.put("status", isSuccess);
		
		if(isSuccess && null != received) {
			logger.debug("File received successfully: " + received.getHead().get("name").toString());
		} else {
			if(null == head.get("name")) {
				logger.debug("File is not received");
			} else {
				logger.debug("File is not received: " + head.get("name").toString());
			}
		}
		
		try {
			connection.sendMessage(new Message(head, null));
		} catch (IOException e) {
			logger.warn("Sending of result failed");
			e.printStackTrace();
		}
	}
	
	private void stopSpeedMeasurer() {
		if(speedMeasurer != null) {
			speedMeasurer.interrupt();
		}
	}
}
