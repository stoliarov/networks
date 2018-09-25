package ru.nsu.stoliarov.task2.usage;

import ru.nsu.stoliarov.task2.client.ClientApp;

import java.nio.file.Path;

public class Client1 {
	public static void main(String[] args) {
		ClientApp clientApp = new ClientApp();
		clientApp.sendFile("192.168.1.4", 4000, "./Setup.exe");
	}
}
