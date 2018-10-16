package ru.nsu.stoliarov.task3.app;

import java.net.InetAddress;

public class NodeInfo {
	private String name;
	private InetAddress address;
	private int port;
	private String id;
	
	public NodeInfo(InetAddress address, int port) {
		this.address = address;
		this.port = port;
		this.id = getIdByAddress(address, port);
		this.name = null;
	}
	
	public NodeInfo(InetAddress address, int port, String name) {
		this.name = name;
		this.address = address;
		this.port = port;
		this.id = getIdByAddress(address, port);
	}
	
	public static String getIdByAddress(InetAddress address, int port) {
		return address.getHostAddress() + port;
	}
	
	public boolean containName() {
		return null != this.name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
}
