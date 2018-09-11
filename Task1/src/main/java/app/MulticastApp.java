package app;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;

import java.util.ArrayList;
import java.util.List;

public class MulticastApp extends Thread {
	private static final Logger logger = LogManager.getLogger(MulticastApp.class.getName());
	
	private InetAddress group;
	private MulticastSocket multicastSocket;
	private int port;
	private byte[] buffer = new byte[1024];
	private byte[] inBuffer = new byte[1024];
	private List<String> copies = new ArrayList<String>();
	
	public MulticastApp(String groupIP, int port) {
		try {
			this.port = port;
			this.group = InetAddress.getByName(groupIP);
			this.multicastSocket = new MulticastSocket(port);
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
			
			while(true) {
				System.out.println("-----------Round----------");
				JSONObject inMessage = receiveMessage();
				System.out.println("Received: " + inMessage.toString());
				
				if(null != inMessage) {
					String event = inMessage.get("event").toString();
					if("leave".equals(event)) {
						System.out.println("Success");
						// to do remove from array
					} else if("join".equals(event)) {
						// add to array
					}
					
					if(random50()) {
						break;
					}
				} else {
					logger.error("Fail to parse the received message");
				}
			}
			
			leaveGroup();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
		Receives a message from the multicast group and returns it as JSONObject or null if an error of JSON parsing occurred.
	 */
	private JSONObject receiveMessage() throws IOException {
		DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);
		multicastSocket.receive(packet);
		String inMessage = new String(packet.getData(), 0, packet.getLength());
		System.out.println("ip: " + multicastSocket.getLocalSocketAddress());
		JSONParser parser = new JSONParser();
		try {
			return (JSONObject) parser.parse(inMessage);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void leaveGroup() throws IOException {
		sendMessage(Event.LEAVE);
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
		} else {
			outMessage.put("ip", multicastSocket.getLocalAddress().toString());
			System.out.println("Sent: " + outMessage.toString());
		}
		
		buffer = outMessage.toString().getBytes();
		
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
		multicastSocket.send(packet);
	}
	
	/*
		Returns true or false with a chance of 50%.
    */
	private boolean random50() {
		return ((int) Math.random() * 10 >= 5);
	}
	
	private enum Event {
		JOIN {
			@Override
			public String toString() {
				return "join";
			}
		},
		LEAVE {
			@Override
			public String toString() {
				return "leave";
			}
		},
		SHOW_LIST {
			@Override
			public String toString() {
				return "show_list";
			}
		}
	}
}
