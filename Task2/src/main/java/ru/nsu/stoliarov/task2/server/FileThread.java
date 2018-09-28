package ru.nsu.stoliarov.task2.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class FileThread extends Thread {
	private static final Logger logger = LogManager.getLogger(FileThread.class.getName());
	
	private final String DIRECTORY = "./uploads/";
	
	private LinkedBlockingQueue<Task> taskQueue;
	private Map<String, FileOutputStream> fileStreams;
	
	public FileThread(LinkedBlockingQueue<Task> tasks) {
		this.taskQueue = tasks;
		fileStreams = new HashMap<>();
	}
	
	@Override
	public void run() {
		while(true) {
			Task task;
			
			try {
				task = taskQueue.take();
				if(!fileStreams.containsKey(task.getFileName())) {
					fileStreams.put(task.getFileName(), new FileOutputStream(DIRECTORY + task.getFileName(), false));
				}
				
				fileStreams.get(task.getFileName()).write(task.getData());
				
				if(task.isLast()) {
					fileStreams.get(task.getFileName()).close();
					fileStreams.remove(task.getFileName());
				}
				
			} catch (InterruptedException e) {
				logger.debug("File thread is interrupted.");
				return;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
