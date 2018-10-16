package ru.nsu.stoliarov.task3.app;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task3.JsonParser;
import ru.nsu.stoliarov.task3.message.Event;
import ru.nsu.stoliarov.task3.message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Receiver implements Runnable {
	private static final Logger logger = LogManager.getLogger(Receiver.class.getName());
	
	private DatagramSocket socket;
	private String name;
	private LinkedBlockingQueue<Message> messagesToSend;
	private ConcurrentMap<String, Message> expectedConfirmation;
	private ConcurrentMap<String, NodeInfo> neighbors;
	private LinkedHashMap<String, String> lastMessages;
	private Random random;
	private int lossPercentage;
	
	public Receiver(int port,
	                String name,
	                LinkedBlockingQueue<Message> messagesToSend,
	                ConcurrentMap<String, Message> expectedConfirmation,
	                ConcurrentMap<String, NodeInfo> neighbors,
	                int lossPercentage) throws SocketException {
		
		this.socket = new DatagramSocket(port);
		this.expectedConfirmation = expectedConfirmation;
		this.messagesToSend = messagesToSend;
		this.lastMessages = new LinkedHashMap<>();
		this.neighbors = neighbors;
		this.name = name;
		this.lossPercentage = lossPercentage;
		this.random = new Random();
	}
	
	@Override
	public void run() {
		Timer confirmationController = new Timer();
		confirmationController.schedule(new ConfirmationsControl(), Settings.CONFIRMATION_TIMEOUT, Settings.CONFIRMATION_TIMEOUT);
		
		while(true) {
			receiveMessage();
			
			if(Thread.interrupted()) {
				finishWork();
				return;
			}
		}
	}
	
	private void receiveMessage() {
		byte[] buffer = new byte[Settings.BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JSONObject received = JsonParser.getJSONbyBytes(packet.getData(), 0, packet.getLength());

		if(random.nextInt(100) < this.lossPercentage) {
			logger.debug("Message ignored: " + received);
			return;
		}
		
		printMessage(packet, received);
		saveMessage(packet, received);
		parseMessage(packet, received);
		// todo потестить отправку кучи сообщений: нужно ли усложнять буффер для входящих сообщений?
		// todo хранение послдених 100 сообщений
	}
	
	private void parseMessage(DatagramPacket packet, JSONObject received) {
		if(received.get("event").toString().equals(Event.HI.toString())) {
			parseAsHi(packet, received);
			
		} else if(received.get("event").toString().equals(Event.MESSAGE.toString())) {
			parseAsMessage(packet, received);
			
		} else if(received.get("event").toString().equals(Event.CONFIRM.toString())) {
			parseAsConfirm(packet, received);
		}
	}
	
	private void printMessage(DatagramPacket packet, JSONObject received) {
		if(received.get("event").equals(Event.MESSAGE.toString()) && !lastMessages.containsKey(
				Message.getIdByAddressAndGUID(
						packet.getAddress(),
						Integer.valueOf(received.get("port").toString()),
						(String) received.get("guid")
				))
		) {
			System.out.println("Получено: '" + received.get("text") + "' от " + received.get("name"));
		}
		if(lastMessages.containsKey(
				Message.getIdByAddressAndGUID(
						packet.getAddress(),
						Integer.valueOf(received.get("port").toString()),
						(String) received.get("guid")
				))
		) {
			logger.debug("Message got (twice or more): " + received.toString());
		} else {
			logger.debug("Message got: " + received.toString());
		}
	}
	
	private void saveMessage(DatagramPacket packet, JSONObject received) {
		if(lastMessages.size() > 1000) {
			Set keys = lastMessages.keySet();
			Object[] keysArray = keys.toArray();
			String firstKey = (String) keysArray[0];
			lastMessages.remove(firstKey);
		}
		lastMessages.put(
				Message.getIdByAddressAndGUID(
						packet.getAddress(),
						Integer.valueOf(received.get("port").toString()),
						(String) received.get("guid")
				),
				""
		);
	}
	
	private void parseAsConfirm(DatagramPacket packet, JSONObject received) {
		if(!expectedConfirmation.containsKey(
				Message.getIdByAddressAndGUID(
						packet.getAddress(),
						Integer.valueOf(received.get("port").toString()),
						(String) received.get("guid")
				))) {
			
			logger.warn("Got unexpected confirmation: " + received.toString());
		}
		expectedConfirmation.remove(
				Message.getIdByAddressAndGUID(
						packet.getAddress(),
						Integer.valueOf(received.get("port").toString()),
						(String) received.get("guid")
				)
		);
	}
	
	private void parseAsMessage(DatagramPacket packet, JSONObject received) {
		String senderID = NodeInfo.getIdByAddress(packet.getAddress(), Integer.valueOf(received.get("port").toString()));
		
		if(!neighbors.containsKey(senderID)) {
			String senderName = (String) received.get("name");
			int senderPort = Integer.valueOf(received.get("port").toString());
			neighbors.put(senderID, new NodeInfo(packet.getAddress(), senderPort, senderName));
		}
		
		messagesToSend.offer(
				new Message(
						Event.CONFIRM,
						(String) received.get("guid"),
						packet.getAddress(),
						neighbors.get(senderID).getPort()
				)
		);
		neighbors.forEach((k, v) -> {
			if(!v.getId().equals(senderID)) {
				Message message = new Message(
						Event.MESSAGE,
						(String) received.get("text"),
						(String) received.get("name"),
						(String) received.get("guid"),
						v.getAddress(),
						v.getPort()
				);
				messagesToSend.offer(message);
				expectedConfirmation.put(message.getId(), message);
			}
		});
	}
	
	private void parseAsHi(DatagramPacket packet, JSONObject received) {
		String senderName = (String) received.get("name");
		String senderID = NodeInfo.getIdByAddress(packet.getAddress(), Integer.valueOf(received.get("port").toString()));
		
		neighbors.put(senderID, new NodeInfo(packet.getAddress(), Integer.valueOf(received.get("port").toString()), senderName));
		if(!neighbors.get(senderID).containName()) {
			neighbors.get(senderID).setName(senderName);
		}
		
		messagesToSend.offer(
				new Message(
						Event.CONFIRM,
						(String) received.get("guid"),
						packet.getAddress(),
						neighbors.get(senderID).getPort()
				)
		);
	}
	
	private void finishWork() {
		if(null != socket) socket.close();
	}
	
	private class ConfirmationsControl extends TimerTask {
		@Override
		public void run() {
			expectedConfirmation.forEach((id, message) -> {
				if(message.isSent()) {
					if(System.currentTimeMillis() - message.getTime() > Settings.CONFIRMATION_TIMEOUT * 3) {
						neighbors.remove(NodeInfo.getIdByAddress(message.getAddress(), message.getPort()));
						
					} else if(System.currentTimeMillis() - message.getTime() > Settings.CONFIRMATION_TIMEOUT) {
						messagesToSend.offer(message);
					}
				}
			});
		}
	}
}
