package ru.nsu.stoliarov.task2.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.nsu.stoliarov.task2.Connection;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Session extends Thread {
	private static final Logger logger = LogManager.getLogger(Session.class.getName());
	
	private LinkedBlockingQueue<Task> tasks;
	Connection connection;
	
	private int fileSize;       // max 1 099 511 627 776 byte
	private String fileName;    // max 4096 byte
	
	public Session(LinkedBlockingQueue<Task> tasks, Socket socket) throws IOException {
		this.tasks = tasks;
		this.connection = new Connection(socket.getInputStream(), socket.getOutputStream(), socket);
	}
	
	@Override
	public void run() {
		try {
			getHi();
			getMetadata();
			getData();
			
		} catch (IOException e) {
			e.printStackTrace();
			connection.closeConnection();
		}
	}
	
	// todo заполняем методы получения HI, METADATA, DATA
	// todo организовать отправку сообщения об успехе передачи файла
	// todo вывод скорости приема
	
	private void getHi() throws IOException {
	
	}
	
	private void getMetadata() {
	
	}
	
	private void getData() {
		while(true) {
		
		}
	}
}
