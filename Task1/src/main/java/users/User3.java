package users;

import app.MulticastApp;

import java.io.IOException;

public class User3 {
	public static void main(String[] args) {
		Thread app = null;
		try {
			app = new MulticastApp("230.0.0.0", 4001);
		} catch (IOException e) {
			e.printStackTrace();
		}
		app.run();
	}
}
