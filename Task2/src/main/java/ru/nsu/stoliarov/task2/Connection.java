package ru.nsu.stoliarov.task2;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;

public class Connection {
	private static final Logger logger = LogManager.getLogger(Connection.class.getName());
	
	public final int BUFFER_SIZE = 8192;
	public final int HEAD_SIZE = 92;
	public final int DATA_SIZE = BUFFER_SIZE - HEAD_SIZE;
	
	private BufferedInputStream inStream;
	private BufferedOutputStream outStream;
	private Socket socket;
	
	public Connection(InputStream inStream, OutputStream outStream, Socket socket) {
		this.inStream = new BufferedInputStream(inStream);
		this.outStream = new BufferedOutputStream(outStream);
		this.socket = socket;
	}
	
	public void sendMessage(Message message) throws IOException {
		byte[] bufferToSend = new byte[BUFFER_SIZE];
		
		if(null == message.getData()) {
			byte[] head = message.getHead().toString().getBytes();
			System.arraycopy(head, 0, bufferToSend, 0, head.length);
			
		} else {
			if(message.getHead().toString().length() > HEAD_SIZE) {
				throw new IOException("Unable to send the message. Size of head of message (" + message.getHead().toString().length()
						+ ") more than maximum head size (" + HEAD_SIZE + ")." + " This head: " + message.getHead().toString());
			}
			
			byte[] head = message.getHead().toString().getBytes();
			
			System.arraycopy(head, 0, bufferToSend, 0, head.length);
			System.arraycopy(message.getData(), 0, bufferToSend, HEAD_SIZE, message.getData().length);
		}
		
		outStream.write(bufferToSend);
		outStream.flush();
	}
	
	public Message receiveMessage(Event expected) throws IOException {
		
		JSONObject head;
		byte[] headBytes;
		byte[] data = null;
		
		if(expected.toString().equals(Event.DATA.toString())) {
			headBytes = readHead();
			
			String headString = new String(headBytes, 0, HEAD_SIZE);
			headString = headString.trim();
			
			head = JsonParser.getJSONbyString(headString);
			checkHead(head, expected);
			
			data = read(Integer.valueOf(head.get("size").toString()));
			
		} else {
			headBytes = read(BUFFER_SIZE);
			
			String headString = new String(headBytes, 0, headBytes.length);
			headString = headString.trim();
			
			head = JsonParser.getJSONbyString(headString);
			checkHead(head, expected);
		}
		
//		logger.debug("Got:" + head.toString() + ".");
		
		return new Message(head, data);
	}
	
	private void checkHead(JSONObject head, Event expected) throws IOException {
		
		if(!head.containsKey("event")) {
			throw new IOException("Received the message without specified type");
		}
		if(!expected.toString().equals(head.get("event").toString())) {
			throw new IOException("Received unexpected type of message: " + head.get("event")
					+ ". Expected: " + expected.toString());
		}
	}
	
	private byte[] readAll() throws IOException {
		int i = 0;
		byte firstByte = (byte) inStream.read();
		int available = inStream.available();
		byte[] inBuffer = new byte[available + 1];
		inBuffer[0] = firstByte;
		while(available > 0) {
			inBuffer[++i] = (byte) inStream.read();
			--available;
		}
		
		return inBuffer;
	}
	
	private byte[] readHead() throws IOException {
		byte firstByte = (byte) inStream.read();
		int available = inStream.available();

		if(available < HEAD_SIZE) throw new IOException("Too short head size");
		
		byte[] inBuffer = new byte[HEAD_SIZE];
		inBuffer[0] = firstByte;
		for(int i = 1; i < HEAD_SIZE; ++i) {
			inBuffer[i] = (byte) inStream.read();
		}
		
		return inBuffer;
	}
	
	private byte[] read(int size) throws IOException {
		byte[] inBuffer = new byte[size];
		int count = 0;
		
		while(count < size) {
			inBuffer[count++] = (byte) inStream.read();
		}
		
		return inBuffer;
	}
	
	/**
	 * Closes IN/OUT streams and socket of this session.
	 */
	public void closeConnection() {
		try {
			inStream.close();
			outStream.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
