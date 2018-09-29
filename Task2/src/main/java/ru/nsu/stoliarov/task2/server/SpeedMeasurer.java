package ru.nsu.stoliarov.task2.server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class SpeedMeasurer extends Thread {
	private final int SHOW_SPEED_TIMEOUT = 1000;
	private final int CALCULATE_SPEED_TIMEOUT = 500;
	
	private LinkedBlockingQueue<Long> messageLengths;
	private long totalLength = 0;
	private long partialLength = 0;
	
	private long initialTime;
	private long lastCalcTime;   // time the last calculation of speed
	private double currentSpeed;
	private double averageSpeed;
	
	
	public SpeedMeasurer(LinkedBlockingQueue<Long> messageLengths, long initialTime) {
		this.messageLengths = messageLengths;
		this.initialTime = initialTime;
		this.lastCalcTime = this.initialTime;
	}
	
	@Override
	public void run() {
		Timer speedCalculator = new Timer();
		speedCalculator.schedule(new CalculateSpeed(), CALCULATE_SPEED_TIMEOUT, CALCULATE_SPEED_TIMEOUT);
		
		Timer speedIndicator = new Timer();
		speedIndicator.schedule(new ShowSpeed(), SHOW_SPEED_TIMEOUT, SHOW_SPEED_TIMEOUT);
		
		while(true) {
			try {
				long messageLength = messageLengths.take();
				totalLength += messageLength;
				partialLength += messageLength;
				
			} catch (InterruptedException e) {
				speedIndicator.cancel();
				speedCalculator.cancel();
				return;
			}
		}
	}
	
	private class CalculateSpeed extends TimerTask {
		@Override
		public void run() {
			// todo попробовать убрать каст к double
			currentSpeed = (double) partialLength / (System.currentTimeMillis() - lastCalcTime);
			averageSpeed = (double) totalLength / (System.currentTimeMillis() - initialTime);
			
			partialLength = 0;
			lastCalcTime = System.currentTimeMillis();
			
			// to Kb/s
			currentSpeed *= 1000;
			currentSpeed /= 8192;
			averageSpeed *= 1000;
			averageSpeed /= 8192;
		}
	}
	
	private class ShowSpeed extends TimerTask {
		@Override
		public void run() {
			System.out.format("Current speed: %.1f Kb/s %n", currentSpeed);
			System.out.format("Average speed: %.1f Kb/s %n%n", averageSpeed);
		}
	}
}
