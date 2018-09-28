package ru.nsu.stoliarov.task2.usage;

import ru.nsu.stoliarov.task2.client.ClientApp;

public class Client2 {
	public static void main(String[] args) {
		ClientApp clientApp = new ClientApp();
		clientApp.sendFile("192.168.43.211", 4000, "./to_send/laba2");
	}
}
