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
			sendMessage(Event.ALIVE);
			
			Timer confirmationTimer = new Timer();
			confirmationTimer.schedule(new LiveConfirmation(), confirmationTimeout, confirmationTimeout);
			
			Timer listUpdateTimer = new Timer();
			listUpdateTimer.schedule(new UpdatingListOfCopies(), confirmationTimeout, confirmationTimeout);
			
			while(true) {
				DatagramPacket inMessage = receiveMessage();
				logger.debug("\n-----------Round----------");
				
				parseMessage(inMessage);
				
				if(interrupted()) {
					leaveGroup(group);
					return;
				}
			}
			
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
				
				if(event.equals(Event.ALIVE.toString())) {
					parseAlive(packet);
				}
				
			} else {
				logger.error("Fail to parse the received message: " + stringMessage);
			}
		} catch (ParseException e) {
			e.printStackTrace();
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
	 * Sends specified type of message to the multicast group.
	 *
	 * @param event type of message
	 */
	private void sendMessage(Event event) throws IOException {
		JSONObject outMessage = new JSONObject();
		outMessage.put("event", event.toString());
		
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
