package ru.nsu.stoliarov.task7;

import java.util.ArrayList;
import java.util.HashMap;

public class DnsRequest {
	private HashMap<String, Info> requests = new HashMap<>();
	private ArrayList<String> toSend = new ArrayList<>();
	
	public DnsRequest() {
	}
	
	public HashMap<String, Info> getRequests() {
		return requests;
	}
	
	public void setRequests(HashMap<String, Info> requests) {
		this.requests = requests;
	}
	
	public ArrayList<String> getToSend() {
		return toSend;
	}
	
	public void setToSend(ArrayList<String> toSend) {
		this.toSend = toSend;
	}
}
