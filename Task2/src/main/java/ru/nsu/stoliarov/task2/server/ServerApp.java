package ru.nsu.stoliarov.task2.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class ServerApp extends Thread {
	private static final Logger logger = LogManager.getLogger(ServerApp.class.getName());
	
	private int port;
	
	/**
	 * Creates a ServerApp with specified port.
	 * @param port port of the server
	 */
	public ServerApp(int port) {
		this.port = port;
	}
	
	@Override
	public void run() {
		LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue<Task>(100000);
		
		FileThread fileThread = new FileThread(taskQueue);
		fileThread.start();
		
		NetworkThread networkThread = new NetworkThread(port, taskQueue);
		networkThread.start();
		
	}
}
