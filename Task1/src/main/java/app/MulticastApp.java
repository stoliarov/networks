package app;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;

import java.util.*;

public class MulticastApp extends Thread {
	private static final Logger logger = LogManager.getLogger(MulticastApp.class.getName());
	
	private InetAddress group;
	private MulticastSocket multicastSocket;
	private DatagramSocket datagramSocket;
	private int port;
	private byte[] buffer = new byte[1024];
	private byte[] inBuffer = new byte[1024];
	private Map<String, String> copies = new HashMap<String, String>();
	
	private int confirmationTimeout = 3000;
	
	
	public MulticastApp(String groupIP, int port) {
		try {
			this.port = port;
			this.group = InetAddress.getByName(groupIP);
			this.multicastSocket = new MulticastSocket(port);
			this.datagramSocket = new DatagramSocket();
//			multicastSocket.setSoTimeout(10000);
		
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			joinGroup();
			Timer timer = new Timer();
			timer.schedule(new LiveConfirmationTask(), confirmationTimeout, confirmationTimeout);
			
			while(true) {
				System.out.println("-----------Round----------");
				DatagramPacket inMessage = receiveMessage();
				
				System.out.println("Received: " + inMessage.toString());
				
				parseMessage(inMessage);
				
				if(random10()) {
					break;
				}
			}
			
			leaveGroup();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses specified message and does something depends on type of the message.
	 * @param packet message to parse
	 */
	private void parseMessage(DatagramPacket packet) {
		String stringMessage = new String(packet.getData(), 0, packet.getLength());
		JSONParser parser = new JSONParser();
		
		try {
			JSONObject jsonMessage = (JSONObject) parser.parse(stringMessage);
			
			if(null != jsonMessage) {
				String event = jsonMessage.get("event").toString();
				
				if(event.equals(Event.JOIN.toString())) {
					copies.put(packet.getAddress().toString() + String.valueOf(packet.getPort()),
							packet.getAddress().toString());
					
				} else if(event.equals(Event.ALIVE.toString())) {
					copies.remove()
				}
				
			} else {
				logger.error("Fail to parse the received message");
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	/**
		Receives a message from the multicast group and returns it as DatagramPacket.
	 */
	private DatagramPacket receiveMessage() throws IOException {
		DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);
		multicastSocket.receive(packet);
		return packet;
	}
	
	private void confirmLife() {
	
	}
	
	private void leaveGroup() throws IOException {
		multicastSocket.leaveGroup(group);
		multicastSocket.close();
	}
	
	private void joinGroup() throws IOException {
		multicastSocket.joinGroup(group);
		sendMessage(Event.JOIN);
	}
	
	private void sendMessage(Event event) throws IOException {
		JSONObject outMessage = new JSONObject();
		outMessage.put("event", event.toString());
		
		if(event.equals(Event.SHOW_LIST)) {
			// TODO put the whole list of known ip
		}
		System.out.println("Sent: " + outMessage.toString());
		
		buffer = outMessage.toString().getBytes();
		
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
		datagramSocket.send(packet);
	}
	
	/*
		Returns true or false with a chance of 10%.
    */
	private boolean random10() {
		return ((int) Math.random() * 10 >= 9);
	}
	
	private enum Event {
		JOIN {
			@Override
			public String toString() {
				return "join";
			}
		},
		ALIVE {
			@Override
			public String toString() {
				return "alive";
			}
		},
		SHOW_LIST {
			@Override
			public String toString() {
				return "show_list";
			}
		}
	}
	
	private class LiveConfirmationTask extends TimerTask {
		@Override
		public void run () {
			try {
				sendMessage(Event.ALIVE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
