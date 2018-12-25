package ru.nsu.stoliarov.task7;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class Info {
	private boolean setupMode;
	private boolean readyToReceiveData;
	private int step;
	private SelectionKey clientKey;
	private SelectionKey ServerKey;
	private int serverPort;
	private int domainNameLength;
	private ByteBuffer buffer = ByteBuffer.allocate(Settings.BUFFER_SIZE);
	
	Info(SelectionKey client, SelectionKey server, boolean setupMode) {
		clientKey = client;
		ServerKey = server;
		this.setupMode = setupMode;
		this.step = 0;
		this.readyToReceiveData = false;
	}
	
	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	
	public ByteBuffer getBuffer() {
		return buffer;
	}
	
	public SelectionKey getClientKey() {
		return clientKey;
	}
	
	public void setClientKey(SelectionKey clientKey) {
		this.clientKey = clientKey;
	}
	
	public SelectionKey getServerKey() {
		return ServerKey;
	}
	
	public void setServerKey(SelectionKey serverKey) {
		ServerKey = serverKey;
	}
	
	public boolean isSetupMode() {
		return setupMode;
	}
	
	public void setSetupMode(boolean setupMode) {
		this.setupMode = setupMode;
	}
	
	public int getStep() {
		return step;
	}
	
	public void setStep(int step) {
		this.step = step;
	}
	
	public void increaseStep() {
		step++;
	}
	
	public boolean isReadyToReceiveData() {
		return readyToReceiveData;
	}
	
	public void setReadyToReceiveData(boolean readyToReceiveData) {
		this.readyToReceiveData = readyToReceiveData;
	}
	
	public int getServerPort() {
		return serverPort;
	}
	
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	
	public int getDomainNameLength() {
		return domainNameLength;
	}
	
	public void setDomainNameLength(int domainNameLength) {
		this.domainNameLength = domainNameLength;
	}
}
