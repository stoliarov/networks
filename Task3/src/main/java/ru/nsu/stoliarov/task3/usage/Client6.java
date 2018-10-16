package ru.nsu.stoliarov.task3.usage;

import ru.nsu.stoliarov.task3.app.App;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Client6 {
	public static void main(String[] args) {
		try {
			App app = new App("Client6", 3, 4006, "localhost", 4005);
			app.run();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
