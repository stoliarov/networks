package ru.nsu.stoliarov.task2.usage;

import ru.nsu.stoliarov.task2.server.ServerApp;

public class Server {
	public static void main(String[] args) {
		ServerApp serverApp = new ServerApp(4000);
		serverApp.start();
	}
}
