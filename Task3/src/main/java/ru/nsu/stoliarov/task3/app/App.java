package ru.nsu.stoliarov.task3.app;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.nsu.stoliarov.task3.message.Event;
import ru.nsu.stoliarov.task3.message.Message;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class App implements Runnable {
	private static final Logger logger = LogManager.getLogger(App.class.getName());
	
	private Thread sender;
	private Thread receiver;
	
	private ConcurrentMap<String, NodeInfo> neighbors;
	private LinkedBlockingQueue<Message> messagesToSend;
	private ConcurrentMap<String, Message> expectedConfirmation;
	private String name;
	private InetAddress parentAddress;
	private int parentPort;
	
	public App(String name, int lossPercentage, int port) throws SocketException {
		this.messagesToSend = new LinkedBlockingQueue<>(10000);
		this.expectedConfirmation = new ConcurrentHashMap<>();
		this.neighbors = new ConcurrentHashMap<>();
		
		this.sender = new Thread(new Sender(name, port, messagesToSend, neighbors, expectedConfirmation));
		this.receiver = new Thread(new Receiver(port, name, messagesToSend, expectedConfirmation, neighbors, lossPercentage));
		
		this.name = name;
		this.parentAddress = null;
	}
	
	public App(String name, int lossPercentage, int port, String parentHost, int parentPort) throws UnknownHostException, SocketException {
		this(name, lossPercentage, port);
		this.parentAddress = InetAddress.getByName(parentHost);
		this.parentPort = parentPort;
		NodeInfo parent = new NodeInfo(this.parentAddress, parentPort);
		this.neighbors.put(parent.getId(), parent);
	}
	
	@Override
	public void run() {
		sender.start();
		receiver.start();
		
		if(parentAddress != null) {
			if(!sendHiToParent()) return;
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			logger.debug("Main thread is interrupted.");
			finishWork();
			return;
		}
		
		enterMessagesToSend();
		
		return;
	}
	
	private void enterMessagesToSend() {
		Scanner scanner = new Scanner(System.in);
		
		while(true) {
			printNeighborsList();
			
			System.out.println("Введите сообщение: ");
			String text = scanner.nextLine();
			
			String guid = UUID.randomUUID().toString();
			neighbors.forEach((k, v) -> {
				sendMessage(Event.MESSAGE, text, this.name, v.getId(), guid);
			});
			
			if(Thread.interrupted()) {
				logger.debug("Main thread is interrupted.");
				finishWork();
				return;
			}
		}
	}
	
	private void printNeighborsList() {
		System.out.println("\nСписок доступных соседей:");
		neighbors.forEach((k, v) -> {
			if(v.containName()) {
				System.out.println(v.getName() + " (name)");
			} else {
				System.out.println(v.getId() + " (id)");
			}
		});
		if(!neighbors.isEmpty()) {
			System.out.println();
		}
	}
	
	private boolean sendHiToParent() {
		if(!sendMessage(Event.HI, "", this.name, parentAddress, parentPort, UUID.randomUUID().toString())) {
			logger.error("Failed to send first message to the parent.");
			return false;
		}
		return true;
	}
	
	private boolean sendMessage(Event event, String text, String name, String recipient, String guid) {
		NodeInfo recipientInfo = neighbors.get(recipient);
		return sendMessage(event, text, name, recipientInfo.getAddress(), recipientInfo.getPort(), guid);
	}
	
	private boolean sendMessage(Event event, String text, String name, InetAddress address, int port, String guid) {
		try {
			Message message = new Message(event, text, name, guid, address, port);
			if(!messagesToSend.offer(message, 1, TimeUnit.SECONDS)) {
				
				logger.warn("Queue is busy. Failed to send the message");
				System.out.println("Очередь с сообщениями перегружена. Ожидайте...");
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				
				return false;
			} else {
				expectedConfirmation.put(message.getId(), message);
				return true;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
	
	private void finishWork() {
		sender.interrupt();
		receiver.interrupt();
	}
}
