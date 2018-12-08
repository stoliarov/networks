package ru.nsu.stoliarov.task6;

public class Main {
	public static void main(String[] args) {
		Forwarder forwarder = new Forwarder(10080, "fit.ippolitov.me", 80);
		forwarder.run();
	}
}
