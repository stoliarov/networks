package ru.nsu.stoliarov.task2;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class Connection {
	private static final Logger logger = LogManager.getLogger(Connection.class.getName());
	
	public final int BUFFER_SIZE = 8192;
	public final int HEAD_SIZE = 64;
	
	private BufferedInputStream inStream;
	private BufferedOutputStream outStream;
	private Socket socket;
	
	public Connection(InputStream inStream, OutputStream outStream, Socket socket) {
		this.inStream = new BufferedInputStream(inStream);
		this.outStream = new BufferedOutputStream(outStream);
		this.socket = socket;
	}
	
	public void exchangeMessages(Map<String, String> toSend, Map<String, String> expected, byte[] dataBytes) throws IOException {
		sendMessage(toSend, dataBytes);
		receiveMessage(expected);
	}
	
	private void sendMessage(Map<String, String> toSend, byte[] dataBytes) throws IOException {
		JSONObject jsonToSend = new JSONObject();
		toSend.forEach((k, v) -> {
			jsonToSend.put(k, v);
		});
		
		byte[] bufferToSend;
		
		if(null == dataBytes) {
			bufferToSend = jsonToSend.toString().getBytes();
		} else {
			if(jsonToSend.toString().length() > HEAD_SIZE) {
				throw new IOException("Unable to send the message. Size of head of message (" + jsonToSend.toString().length()
						+ ") more than maximum head size (" + HEAD_SIZE + ")." + " This head: " + jsonToSend.toString());
			}
			
			bufferToSend = new byte[HEAD_SIZE + dataBytes.length];
			byte[] headBytes = jsonToSend.toString().getBytes();
			
			for(int i = 0; i < headBytes.length; ++i) {
				bufferToSend[i] = headBytes[i];
			}
			for(int i = HEAD_SIZE; i < bufferToSend.length; ++i) {
				bufferToSend[i] = dataBytes[i - HEAD_SIZE];
			}
		}
		
		outStream.write(jsonToSend.toString().getBytes());
		outStream.flush();
	}
	
	private void receiveMessage(Map<String, String> expected) throws IOException {
		// todo доделать эту проверку ожидаемого ответа
		// todo подумать как пользоваться этой функцией и что проверять со стороны сервера - там просто наоборот вызывать -> сначала получение, потом отправка
		byte[] receivedBytes = readBytes();
		
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
	
	private byte[] readBytes() throws IOException {
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
