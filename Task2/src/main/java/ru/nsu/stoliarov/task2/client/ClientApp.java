package ru.nsu.stoliarov.task2.client;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import ru.nsu.stoliarov.task2.Connection;
import ru.nsu.stoliarov.task2.Event;
import ru.nsu.stoliarov.task2.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ClientApp {
	private static final Logger logger = LogManager.getLogger(ClientApp.class.getName());
	
	Connection connection;
	
	/**
	 * Sends file to the server with specified host and port.
	 *
	 * @return whether or not the sending was successful
	 */
	public boolean sendFile(String host, int port, String filePath) {
		try {
			File file = new File(filePath);
			
			InetAddress address = InetAddress.getByName(host);
			logger.debug("Connecting...");
			Socket socket = new Socket(address, port);
			logger.debug("Connected");
			this.connection = new Connection(socket.getInputStream(), socket.getOutputStream(), socket);
			
			sendHi();
			sendMetadata(file.getName(), file.length());
			sendData(file);
			receiveResult(file.getName());
			connection.closeConnection();
			
		} catch (IOException e) {
			e.printStackTrace();
			connection.closeConnection();
			logger.debug("The file sent UNsuccessfully");
			return false;
		}
		
		return true;
	}
	
	private void sendHi() throws IOException {
		JSONObject head = new JSONObject();
		head.put("event", Event.HI.toString());
		
		connection.sendMessage(new Message(head, null));
		connection.receiveMessage(Event.HI);
	}
	
	private void sendMetadata(String fileName, long fileSize) throws IOException {
		JSONObject head = new JSONObject();
		head.put("event", Event.METADATA.toString());
		head.put("name", fileName);
		head.put("size", fileSize);

		connection.sendMessage(new Message(head, null));
		Message response = connection.receiveMessage(Event.GOT);
		
		if(Long.valueOf(response.getHead().get("number").toString()) != 0) {
			throw new IOException("Unexpected response after receipt from server. Actual GOT number: "
					+ response.getHead().get("number") + " Expected: 0");
		}
	}
	
	private void sendData(File file) throws IOException {
		try(FileInputStream fileInputStream = new FileInputStream(file)) {
			if(file.length() < connection.DATA_SIZE) {
				byte[] data = new byte[connection.DATA_SIZE];
				int size = fileInputStream.read(data);
				
				sendDataPiece(data, 1, size);
				
			} else {
				for(int i = 0; i < file.length() / connection.DATA_SIZE; ++i) {
					byte[] data = new byte[connection.DATA_SIZE];
					fileInputStream.read(data, 0, connection.DATA_SIZE);
					sendDataPiece(data, i + 1, connection.DATA_SIZE);
				}
				
				if(fileInputStream.available() > 0) {
					byte[] data = new byte[connection.DATA_SIZE];
					int size = fileInputStream.read(data);
					
					sendDataPiece(data, file.length() / connection.DATA_SIZE + 1, size);
				}
			}
		}
	}
	
	private void sendDataPiece(byte[] data, long number, int size) throws IOException {
		JSONObject head = new JSONObject();
		head.put("event", Event.DATA.toString());
		head.put("number", number);
		head.put("size", size);
		
		
		connection.sendMessage(new Message(head, data));
		Message response = connection.receiveMessage(Event.GOT);
		
		if(Long.valueOf(response.getHead().get("number").toString()) != number) {
			throw new IOException("Unexpected response after receipt from server. Actual GOT number: "
					+ response.getHead().get("number") + " Expected: " + number);
		}
	}
	
	private void receiveResult(String fileName) throws IOException {
		JSONObject head = new JSONObject();
		head.put("event", Event.GET_RESULT.toString());
		head.put("name", fileName);
		connection.sendMessage(new Message(head, null));
		
		JSONObject result = connection.receiveMessage(Event.RESULT).getHead();
		if(result.get("event").toString().equals(Event.RESULT.toString()) && Boolean.valueOf(result.get("status").toString()) == (true)) {
			logger.debug("The file sent successfully: " + fileName);
		} else {
			logger.debug("The file sent UNsuccessfully:1");
		}
	}
}
