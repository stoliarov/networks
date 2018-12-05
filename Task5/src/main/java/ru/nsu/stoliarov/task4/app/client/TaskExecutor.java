package ru.nsu.stoliarov.task4.app.client;

import org.apache.http.HttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TaskExecutor implements Runnable {
	private static final Logger logger = LogManager.getLogger(TaskExecutor.class.getName());
	
	private DefaultHttpClient client;
	private LinkedBlockingQueue<Task> tasks;
	private LinkedBlockingQueue<Result> userResults;
	private ConcurrentHashMap<String, Result> systemResults;
	
	public TaskExecutor(LinkedBlockingQueue<Task> tasks,
	                    LinkedBlockingQueue<Result> userResults,
	                    ConcurrentHashMap<String, Result> systemResults) {
		this.tasks = tasks;
		this.userResults = userResults;
		this.systemResults = systemResults;
		this.client = new DefaultHttpClient();
	}
	
	@Override
	public void run() {
		while(true) {
			Task task;
			try {
				task = tasks.take();
			} catch (InterruptedException e) {
				logger.info("TaskExecutor is interrupted");
				return;
			}
			
			Result result;
			try {
				result = task.getCommand().execute(task.getParams());
			} catch (Exception e) {
				logger.warn("Failed to connect to the server");
				e.printStackTrace();
				result = new Result(1, task.getCommand().getName(), "Не удалось подключиться к серверу");
			}
			
			if(task.isUserTask()) {
				if(!userResults.offer(result)) {
					logger.warn("Failed to get the result of command execution. The userResults queue is busy.");
				}
			} else {
				systemResults.put(result.getTaskName(), result);
			}
		}
	}
}
