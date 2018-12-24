package ru.nsu.stoliarov.task7;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		try {
			Proxy proxy = new Proxy(10080);
			proxy.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
