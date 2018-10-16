package ru.nsu.stoliarov.task3.usage;

import ru.nsu.stoliarov.task3.app.App;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Client2 {
	public static void main(String[] args) {
		try {
			App app = new App("Client2", 3, 4002, "localhost", 4001);
			app.run();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
