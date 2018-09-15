package users;

import app.MulticastApp;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;

public class User1 {
	private static final Logger logger = LogManager.getLogger(User1.class.getName());
	
	public static void main(String[] args) {
		System.out.println("Expected 2 arguments: multicast group IP, port");
		
		Thread app;
		
		try {
			if(args.length > 1) {
				try {
					app = new MulticastApp(args[0], Integer.valueOf(args[1]));
					
				} catch (IOException e) {
					System.out.println("You've specified incorrect IP address. Will uses default IP and port: 230.0.0.0:4001");
					app = new MulticastApp("230.0.0.0", 4001);
					
				} catch (IllegalArgumentException e) {
					System.out.println("You've specified incorrect port. Will uses default IP and port: 230.0.0.0:4001");
					app = new MulticastApp("230.0.0.0", 4001);
				}
			} else {
				System.out.println("Will uses default IP and port: 230.0.0.0:4001");
				app = new MulticastApp("230.0.0.0", 4001);
			}
			
		} catch (IOException e) {
			logger.fatal("Incorrect default IP address or port");
			e.printStackTrace();
			return;
		}
		
		app.run();
	}
}
