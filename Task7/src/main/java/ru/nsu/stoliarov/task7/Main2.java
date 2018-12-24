package ru.nsu.stoliarov.task7;

import java.io.IOException;

public class Main2 {
	public static void main(String[] args) {
		try {
			Proxy2 proxy2 = new Proxy2(10050);
			proxy2.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
