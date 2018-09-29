package ru.nsu.stoliarov.task2.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkThread extends Thread {
	private static final Logger logger = LogManager.getLogger(NetworkThread.class.getName());
	
	private LinkedBlockingQueue<Task> tasks;
	private int port;
	private ServerSocket serverSocket;
	
	public NetworkThread(int port, LinkedBlockingQueue<Task> tasks) {
		this.tasks = tasks;
		this.port = port;
		
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				logger.debug("Waiting for connect...");
				Socket socket = serverSocket.accept();
				logger.debug("Connected: " + socket.getInetAddress() + ":" + socket.getPort());
				
				Session session = new Session(tasks, socket);
				session.start();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
