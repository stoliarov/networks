package ru.nsu.stoliarov.task6;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		try {
			Forwarder forwarder = new Forwarder(10080, "fit.ippolitov.me", 80);
			forwarder.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
