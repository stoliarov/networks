package app;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application that counters a copy of itself in a local network through multicast UDP messages sending.
 * When an application copy appears or disappears this app prints a list of active application copies.
 */
public class MulticastApp extends Thread {
	private static final Logger logger = LogManager.getLogger(MulticastApp.class.getName());
	
	private InetAddress group;
	private MulticastSocket receiverSocket;
	private DatagramSocket senderSocket;
	private int portToSend;
	private byte[] buffer = new byte[1024 * 1024];
	private byte[] inBuffer = new byte[1024 * 1024];
	private Map<String, AppInfo> copies = new ConcurrentHashMap<String, AppInfo>();
	
	private final int confirmationTimeout = 3000;
	
	/**
	 * Allocates a MulticastApp object
	 *
	 * @param groupIP IP-address of multicast group for messaging with other copies of this app.
	 * @param port port of multicast group
	 *
	 * @throws IOException when specified incorrect group IP (e.g. nonexistent or non-multicast)
	 * @throws IllegalArgumentException when specified incorrect port
	 */
	public MulticastApp(String groupIP, int port) throws IOException, IllegalArgumentException {
		this.portToSend = port;
		System.out.println(groupIP);
		this.group = InetAddress.getByName(groupIP);
		
		this.receiverSocket = new MulticastSocket(port);
		this.senderSocket = new DatagramSocket();
		
		receiverSocket.joinGroup(group);
	}
	
	@Override
	public void run() {
		try {
			sendMessage(Event.JOIN);
			
			Timer confirmationTimer = new Timer();
			confirmationTimer.schedule(new LiveConfirmation(), confirmationTimeout, confirmationTimeout);
			
			Timer listUpdateTimer = new Timer();
			listUpdateTimer.schedule(new UpdatingListOfCopies(), confirmationTimeout, confirmationTimeout);
			
			while(true) {
				DatagramPacket inMessage = receiveMessage();
				logger.debug("\n-----------Round----------");
				
				parseMessage(inMessage);
				
				if(random10()) {
					break;
				}
			}
			
			leaveGroup(group);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses specified message and does something depends on type of the message.
	 *
	 * @param packet message to parse
	 */
	private void parseMessage(DatagramPacket packet) throws IOException {
		String stringMessage = new String(packet.getData(), 0, packet.getLength());
		JSONParser parser = new JSONParser();
		
		try {
			JSONObject jsonMessage = (JSONObject) parser.parse(stringMessage);
			
			logger.debug("Received: " + jsonMessage.toString());
			
			if(null != jsonMessage) {
				String event = jsonMessage.get("event").toString();
				
				if(event.equals(Event.JOIN.toString())) {
					parseJoin(packet);
				} else if(event.equals(Event.ALIVE.toString())) {
					parseAlive(packet);
				} else if(event.equals(Event.SHOW_LIST.toString())) {
					parseShowList(jsonMessage);
				}
				
			} else {
				logger.error("Fail to parse the received message");
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses "show_list" type of message.
	 *
	 * @param jsonMessage message to parse
	 */
	private void parseShowList(JSONObject jsonMessage) {
		if((long) jsonMessage.get("sizeOfAppList") > copies.size()) {
			copies.clear();
			JSONArray jsonAppCopies = (JSONArray) jsonMessage.get("copies");
			
			jsonAppCopies.forEach(v -> {
				JSONObject jsonAppCopy = (JSONObject) v;
				
				String ip = jsonAppCopy.get("ip").toString();
				String port = jsonAppCopy.get("port").toString();
				long lastActiveTime = (long) jsonAppCopy.get("lastActiveTime");
				
				copies.put(ip + port, new AppInfo(ip, port, lastActiveTime));
				
			});
			
			printAddressOfCopies();
		}
	}
	
	/**
	 * Parses "alive" type of message.
	 *
	 * @param packet message to parse
	 */
	private void parseAlive(DatagramPacket packet) {
		if(copies.containsKey(packet.getAddress().toString() + String.valueOf(packet.getPort()))) {
			copies.get(packet.getAddress().toString() + String.valueOf(packet.getPort()))
					.setLastActivityTime(System.currentTimeMillis());
		} else {
			copies.put(packet.getAddress().toString() + String.valueOf(packet.getPort()),
					new AppInfo(packet.getAddress().toString(), String.valueOf(packet.getPort()), System.currentTimeMillis()));
			printAddressOfCopies();
		}
	}
	
	/**
	 * Parses "join" type of message.
	 *
	 * @param packet message to parse
	 */
	private void parseJoin(DatagramPacket packet) throws IOException {
		copies.put(packet.getAddress().toString() + String.valueOf(packet.getPort()),
				new AppInfo(packet.getAddress().toString(), String.valueOf(packet.getPort()), System.currentTimeMillis()));
		
		printAddressOfCopies();
		
		if(copies.size() > 1) {
			sendMessage(Event.SHOW_LIST);
		}
	}
	
	/**
	 * Receives a message from the multicast group and returns it as DatagramPacket.
	 */
	private DatagramPacket receiveMessage() throws IOException {
		DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);
		receiverSocket.receive(packet);
		return packet;
	}
	
	/**
	 * Leaves specified multicast group and closes socket-receiver of this group.
	 *
	 * @param group group to leave
	 */
	private void leaveGroup(InetAddress group) throws IOException {
		receiverSocket.leaveGroup(group);
		receiverSocket.close();
	}
	
	/**
	 * Joins specified multicast group and sends the message to this group that this app is joined.
	 *
	 * @param group group to join
	 */
	private void joinGroup(InetAddress group) throws IOException {
		receiverSocket.joinGroup(group);
		sendMessage(Event.JOIN);
	}
	
	/**
	 * Sends specified type of message to the multicast group.
	 *
	 * @param event type of message
	 */
	private void sendMessage(Event event) throws IOException {
		JSONObject outMessage = new JSONObject();
		outMessage.put("event", event.toString());
		
		if(event.equals(Event.SHOW_LIST)) {
			outMessage.put("sizeOfAppList", copies.size());
			
			JSONArray jsonAppCopies = new JSONArray();
			
			copies.forEach((k, v) -> {
				JSONObject jsonAppCopy = new JSONObject();
				
				jsonAppCopy.put("ip", v.getIp());
				jsonAppCopy.put("port", v.getPort());
				jsonAppCopy.put("lastActiveTime", v.getLastActivityTime());
				
				jsonAppCopies.add(jsonAppCopy);
			});
			
			outMessage.put("copies", jsonAppCopies);
		}
		logger.debug("Sent: " + outMessage.toString());
		
		buffer = outMessage.toString().getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, portToSend);
		
		senderSocket.send(packet);
	}
	
	private void printAddressOfCopies() {
		System.out.println("Actual list of copies:");
		copies.forEach((k, v) -> {
			System.out.println(v.getIp() + ":" + v.getPort());
		});
	}
	
	/*
		Returns true or false with a chance of 10%.
    */
	private boolean random10() {
		return ((int) Math.random() * 10 >= 9);
	}
	
	private class LiveConfirmation extends TimerTask {
		@Override
		public void run() {
			try {
				sendMessage(Event.ALIVE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class UpdatingListOfCopies extends TimerTask {
		@Override
		public void run() {
			copies.forEach((k, v) -> {
				if(System.currentTimeMillis() - v.getLastActivityTime() > confirmationTimeout * 3) {
					copies.remove(k);
					printAddressOfCopies();
				}
			});
		}
	}
	
}
