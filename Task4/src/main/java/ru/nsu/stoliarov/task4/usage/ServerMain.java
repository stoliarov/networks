package ru.nsu.stoliarov.task4.usage;

import io.undertow.Undertow;
import ru.nsu.stoliarov.task4.app.server.Server;

public class ServerMain {
	public static void main(String[] args) {
		Undertow server = Server.createServer();
		server.start();
	}
}
