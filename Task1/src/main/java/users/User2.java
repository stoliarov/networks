package users;

import app.MulticastApp;

public class User2 {
	public static void main(String[] args) {
		Thread app = new MulticastApp("230.0.0.0", 4001);
		app.run();
	}
}
