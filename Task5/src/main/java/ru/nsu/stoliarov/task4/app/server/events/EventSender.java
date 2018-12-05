package ru.nsu.stoliarov.task4.app.server.events;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;

public class EventSender implements Runnable {
	private static final Logger logger = LogManager.getLogger(EventSender.class.getName());
	
	@Override
	public void run() {
		while(true) {
			try {
				Event event = EventService.getEvents().take();
				if(event.hasIgnoredToken()) {
					EventService.getSessions().forEach((id, session) -> {
						try {
							if(null == EventService.getSessionIdByToken(event.getIgnoredToken()) ||
									!EventService.getSessionIdByToken(event.getIgnoredToken()).equals(id)) {
								
								session.getBasicRemote().sendText(event.getDescription());
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				} else {
					EventService.getSessions().forEach((id, session) -> {
						try {
							session.getBasicRemote().sendText(event.getDescription());
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				}
			} catch (InterruptedException e) {
				logger.info("TaskExecutor is interrupted");
				return;
			}
		}
	}
}
