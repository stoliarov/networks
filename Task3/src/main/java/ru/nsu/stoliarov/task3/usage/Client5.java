package ru.nsu.stoliarov.task3.usage;

import ru.nsu.stoliarov.task3.app.App;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Client5 {
	public static void main(String[] args) {
		try {
			App app = new App("Client5", 3, 4005, "localhost", 4004);
			app.run();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
