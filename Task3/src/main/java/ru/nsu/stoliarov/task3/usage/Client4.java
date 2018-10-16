package ru.nsu.stoliarov.task3.usage;

import ru.nsu.stoliarov.task3.app.App;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Client4 {
	public static void main(String[] args) {
		try {
			App app = new App("Client4", 3, 4004, "localhost", 4002);
			app.run();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
