package ru.nsu.stoliarov.task2;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;

public class Connection {
	private static final Logger logger = LogManager.getLogger(Connection.class.getName());
	
	public final int BUFFER_SIZE = 8192;
	public final int HEAD_SIZE = 64;
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
		byte[] bufferToSend;
		
		if(null == message.getData()) {
			bufferToSend = message.getHead().toString().getBytes();
		} else {
			if(message.getHead().toString().length() > HEAD_SIZE) {
				throw new IOException("Unable to send the message. Size of head of message (" + message.getHead().toString().length()
						+ ") more than maximum head size (" + HEAD_SIZE + ")." + " This head: " + message.getHead().toString());
			}
			
			bufferToSend = new byte[HEAD_SIZE + message.getData().length];
			byte[] headBytes = message.getHead().toString().getBytes();
			
			for(int i = 0; i < headBytes.length; ++i) {
				bufferToSend[i] = headBytes[i];
			}
			for(int i = HEAD_SIZE; i < bufferToSend.length; ++i) {
				bufferToSend[i] = message.getData()[i - HEAD_SIZE];
			}
		}
		
		outStream.write(bufferToSend);
		outStream.flush();
	}
	
	public Message receiveMessage(Event expected) throws IOException {
		
		JSONObject head;
		byte[] headBytes;
		byte[] data = null;
		
		if(expected.equals(Event.DATA.toString())) {
			headBytes = readHead();
			head = JsonParser.getJSONbyBytes(headBytes, 0, HEAD_SIZE);
			data = readData();
			
		} else {
			headBytes = readAll();
			head = JsonParser.getJSONbyBytes(headBytes, 0, headBytes.length);
		}
		
		if(null == head) {
			throw new IOException("Fail to parse the received message: "
					+ new String(headBytes, 0, headBytes.length));
		}
		if(!head.containsKey("event")) {
			throw new IOException("Received the message without specified type");
		}
		if(!expected.equals(head.get("event"))) {
			throw new IOException("Received unexpected type of message: " + head.get("event")
					+ ". Expected: " + expected.toString());
		}
		
		logger.debug("Client got:" + head.toString());
		
		return new Message(head, data);
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
	
	private byte[] readData() throws IOException {
		int available = inStream.available();
		if(available < 1) throw new IOException("Message with type DATA contain no data");
		
		byte[] inBuffer = new byte[available];
		for(int i = 0; i < available; ++i) {
			inBuffer[i] = (byte) inStream.read();
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
