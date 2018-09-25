package ru.nsu.stoliarov.task2.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class FileThread extends Thread {
	private static final Logger logger = LogManager.getLogger(FileThread.class.getName());
	
	private LinkedBlockingQueue<Task> taskQueue;
	
	public FileThread(LinkedBlockingQueue<Task> tasks) {
		this.taskQueue = tasks;
	}
	
	@Override
	public void run() {
		while(true) {
			Task task;
			
			try {
				task = taskQueue.take();
			} catch (InterruptedException e) {
				logger.debug("File thread was interrupted.");
				return;
			}
		}
	}
}
