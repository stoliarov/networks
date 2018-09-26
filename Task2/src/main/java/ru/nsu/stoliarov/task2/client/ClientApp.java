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

import static java.lang.Thread.sleep;

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
			Socket socket = new Socket(address, port);
			this.connection = new Connection(socket.getInputStream(), socket.getOutputStream(), socket);
			
			sendHi();
			sendMetadata(file.getName(), file.length());
			sendData(file);
			
			try {
				sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
			connection.closeConnection();
			return false;
		}
		
		connection.closeConnection();
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
		head.put("event", Event.METADATA);
		head.put("name", fileName);
		head.put("size", fileSize);

		connection.sendMessage(new Message(head, null));
		Message response = connection.receiveMessage(Event.GOT);
		
		if((long) response.getHead().get("number") != 0) {
			throw new IOException("Unexpected response after receipt from server. Actual GOT number: "
					+ response.getHead().get("number") + " Expected: 0");
		}
	}
	
	private void sendData(File file) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		
		if(file.length() < connection.DATA_SIZE) {
			sendDataPiece(fileInputStream.readAllBytes(), 1);
			
		} else {
			for(int i = 0; i < file.length() / connection.DATA_SIZE; ++i) {
				byte[] data = new byte[connection.DATA_SIZE];
				fileInputStream.read(data, 0, connection.DATA_SIZE);
				sendDataPiece(data, i + 1);
			}
			
			sendDataPiece(fileInputStream.readAllBytes(), file.length() / connection.DATA_SIZE + 1);
		}
	}
	
	private void sendDataPiece(byte[] data, long number) throws IOException {
		JSONObject head = new JSONObject();
		head.put("event", Event.DATA);
		head.put("number", number);
		
		connection.sendMessage(new Message(head, data));
		Message response = connection.receiveMessage(Event.GOT);
		
		if((long) response.getHead().get("number") != number) {
			throw new IOException("Unexpected response after receipt from server. Actual GOT number: "
					+ response.getHead().get("number") + " Expected: " + number);
		}
	}
}
