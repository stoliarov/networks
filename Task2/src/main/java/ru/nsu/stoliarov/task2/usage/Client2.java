package ru.nsu.stoliarov.task2.usage;

import ru.nsu.stoliarov.task2.client.ClientApp;

public class Client2 {
	public static void main(String[] args) {
		ClientApp clientApp = new ClientApp();
		if(args.length < 3) {
			clientApp.sendFile("localhost", 4000, "./to_send/laba2");
		} else {
			System.out.println("Expected (optional) 3 params: host of the server, port of the server and file to send");
			clientApp.sendFile(args[0], Integer.valueOf(args[1]), args[2]);
		}
	}
}
