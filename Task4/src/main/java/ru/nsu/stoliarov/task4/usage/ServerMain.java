package ru.nsu.stoliarov.task4.usage;

import ru.nsu.stoliarov.task4.app.server.Server;
import java.util.ArrayList;

public class ServerMain {
	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}
}
