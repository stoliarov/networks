package ru.nsu.stoliarov.task3.app;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task3.message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Sender implements Runnable {
	private static final Logger logger = LogManager.getLogger(Sender.class.getName());
	
	private String name;
	private int port;
	private DatagramSocket socket;
	private LinkedBlockingQueue<Message> messagesToSend;
	private ConcurrentMap<String, Message> expectedConfirmation;
	private ConcurrentMap<String, NodeInfo> neighbors;
	
	public Sender(String name,
	              int port,
	              LinkedBlockingQueue<Message> messagesToSend,
	              ConcurrentMap<String, NodeInfo> neighbors,
	              ConcurrentMap<String, Message> expectedConfirmation) throws SocketException {
		
		this.socket = new DatagramSocket();
		this.port = port;
		this.messagesToSend = messagesToSend;
		this.expectedConfirmation = expectedConfirmation;
		this.name = name;
		this.neighbors = neighbors;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				sendMessage(messagesToSend.take());
				
			} catch (InterruptedException e) {
				logger.info("Sender is interrupted");
				socket.close();
				return;
			}
		}
	}
	
	private void sendMessage(Message message) {
		byte[] buffer = new byte[Settings.BUFFER_SIZE];
		
		JSONObject jsonMessage = new JSONObject();
		jsonMessage.put("event", message.getEvent().toString());
		if(message.containText()) {
			jsonMessage.put("text", message.getText());
		}
		jsonMessage.put("guid", message.getGuid());
		if(message.containSenderName()) {
			jsonMessage.put("name", message.getSenderName());
		}
		jsonMessage.put("port", this.port);
		System.arraycopy(jsonMessage.toString().getBytes(), 0, buffer, 0, jsonMessage.toString().getBytes().length);
		
//		System.out.println("Вы шлете: " + jsonMessage.toString());
		
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, message.getAddress(), message.getPort());
		try {
			socket.send(packet);
			logger.debug("Message sent: " + message.getText() + " " + message.getGuid() + " "
					+ message.getPort() + " " + message.getEvent());
			
			if(!message.isSent()) { // that is it's not a resend
				message.sent(System.currentTimeMillis());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
