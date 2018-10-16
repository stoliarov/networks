package ru.nsu.stoliarov.task3.usage;

import ru.nsu.stoliarov.task3.app.App;

import java.net.SocketException;

public class Client1 {
	public static void main(String[] args) {
		try {
			App app = new App("Client1", 100, 4001);
			app.run();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
