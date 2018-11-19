package ru.nsu.stoliarov.task4.app.server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.*;

public class UserActivityHistory {
	private static final Logger logger = LogManager.getLogger(UserActivityHistory.class.getName());
	
	private LinkedHashMap<String, UserActivity> activities;
	private String defaultFirstKey;
	
	public UserActivityHistory() {
		this.activities = new LinkedHashMap<>();
		this.defaultFirstKey = UUID.randomUUID().toString();
		activities.put(defaultFirstKey, new UserActivity("zero activity"));
	}
	
	public synchronized void add(String activityDescription) {
		Set keys = activities.keySet();
		Object[] keysArray = keys.toArray();
		String firstKey = (String) keysArray[0];
		String lastKey = (String) keysArray[keysArray.length - 1];
		
		if(activities.size() > 1000) {
			activities.remove(firstKey);
		}
		
		String key = UUID.randomUUID().toString();
		UserActivity activity = new UserActivity(activityDescription);
		activities.get(lastKey).setNextKey(key);
		activities.put(key, activity);
	}
	
	public synchronized List<String> subHistory(String afterKey) {
		List<String> subHistory = new ArrayList<>();
		if(!activities.containsKey(afterKey)) {
			logger.warn("Unknown key for activity history request " + afterKey);
			return subHistory;
		} else {
			if(afterKey.equals(lastKey()) || afterKey.equals(defaultFirstKey)) {
				return subHistory;
			}
			UserActivity activity = activities.get(activities.get(afterKey).getNextKey());
			while(activity.hasNext()) {
				subHistory.add(activity.getDescription());
				activity = activities.get(activity.getNextKey());
			}
			subHistory.add(activity.getDescription());
			return subHistory;
		}
	}
	
	public synchronized String lastKey() {
		Set keys = activities.keySet();
		Object[] keysArray = keys.toArray();
		return (String) keysArray[keysArray.length - 1];
	}
	
	private class UserActivity {
		private String description;
		private String nextKey = null;
		
		public UserActivity(String description) {
			this.description = description;
		}
		
		public String getDescription() {
			return description;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
		public String getNextKey() {
			return nextKey;
		}
		
		public void setNextKey(String nextKey) {
			this.nextKey = nextKey;
		}
		
		public boolean hasNext() {
			return null != nextKey;
		}
	}
}
