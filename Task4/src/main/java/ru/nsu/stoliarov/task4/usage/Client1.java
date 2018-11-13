package ru.nsu.stoliarov.task4.usage;

import ru.nsu.stoliarov.task4.app.client.Client;

public class Client1 {
	public static void main(String[] args) {
		Client client = new Client("http://localhost:8080");
		client.run();
	}
}
