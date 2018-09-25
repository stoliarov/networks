package ru.nsu.stoliarov.task2.client;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task2.Connection;
import ru.nsu.stoliarov.task2.Event;
import ru.nsu.stoliarov.task2.JsonParser;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

import static java.lang.Thread.sleep;

public class ClientApp {
	private static final Logger logger = LogManager.getLogger(ClientApp.class.getName());
	
	Connection connection;
	
	/**
	 * Sends file to the server with specified host and port.
	 *
	 * @return whether or not the sending was successful
	 */
	public boolean sendFile(String host, int port, String path) {
		try {
			InetAddress address = InetAddress.getByName(host);
			Socket socket = new Socket(address, port);
			
			this.connection = new Connection(socket.getInputStream(), socket.getOutputStream(), socket);
			
			sendHi();
			sendMetadata("filename", 1000);
			sendData();
			
			try {
				sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		connection.closeConnection();
		return true;
	}
	
	private void sendHi() throws IOException {
		JSONObject jsonToSend = new JSONObject();
		jsonToSend.put("event", Event.HI.toString());
		
		outStream.write(jsonToSend.toString().getBytes());
		outStream.flush();
		
		byte[] receivedBytes = readMessage();
		
		JSONObject receivedJson = JsonParser.getJSONbyBytes(receivedBytes, 0, receivedBytes.length);
		System.out.println("Client got:" + receivedJson.toString());
		
		if(null != receivedJson) {
			if(!Event.HI.toString().equals(receivedJson.get("event"))) {
				throw new IOException("Received unexpected type of message: " + receivedJson.get("event")
						+ ". Expected: " + Event.HI.toString());
			}
		} else {
			throw new IOException("Fail to parse the received message: "
					+ new String(receivedBytes, 0, receivedBytes.length));
		}
	}
	
	private void sendMetadata(String fileName, long fileSize) {
		JSONObject jsonMessage = new JSONObject();
		jsonMessage.put("event", Event.METADATA);
		jsonMessage.put("name", fileName);
		jsonMessage.put("size", fileSize);
		
		try {
			outStream.write(jsonMessage.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			// TODO возмножно нужна обработка таких ошибок
		}
	}
	
	private void sendData() {
	
	}
}
