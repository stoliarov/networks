package ru.nsu.stoliarov.task4.app.server.user;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserService {
	private static Logger logger = LogManager.getLogger(UserService.class.getName());
	
	private Map<String, User> users;
	private ConcurrentMap<String, User> activeUsers;
	private Map<String, User> userTokens;
	private Map<Long, User> userIDs;
	
	public UserService() {
		users = new HashMap<>();
		activeUsers = new ConcurrentHashMap<>();
		userTokens = new HashMap<>();
		userIDs = new HashMap<>();
	}
	
	public void addUser(String username) {
		User user = new User(username, users.size());
		users.put(user.getUsername(), user);
		userTokens.put(user.getToken(), user);
		userIDs.put(user.getId(), user);
	}
	
	public boolean login(User user) {
		if(!users.containsKey(user.getUsername())) {
			logger.warn("User not found: " + user);
			return false;
		} else {
			user.setOnline(true);
			activeUsers.put(user.getUsername(), user);
			return true;
		}
	}
	
	public boolean logout(User user) {
		if(!users.containsKey(user.getUsername())) {
			logger.warn("User not found: " + user);
			return false;
		} else {
			user.setOnline(false);
			activeUsers.remove(user.getUsername());
			updateToken(user);
			return true;
		}
	}
	
	public Set<Map.Entry<String, User>> getActiveUsers() {
		return activeUsers.entrySet();
	}
	
	public boolean removeUser(User user) {
		if(!users.containsKey(user.getUsername())) {
			logger.warn("User not found: " + user);
			return false;
		} else {
			users.remove(user.getUsername());
			userTokens.remove(user.getToken());
			userIDs.remove(user.getId());
			activeUsers.remove(user.getUsername());
			return true;
		}
	}
	
	public User getUserByName(String username) {
		return users.get(username);
	}
	
	public User getUserById(long id) {
		return userIDs.get(id);
	}
	
	public User getUserByToken(String token) {
		return userTokens.get(token);
	}
	
	public boolean containsUsername(String username) {
		return users.containsKey(username);
	}
	
	public boolean containsToken(String token) {
		return userTokens.containsKey(token);
	}
	
	public boolean containsId(long id) {
		return userIDs.containsKey(id);
	}
	
	private void updateToken(User user) {
		logger.debug("Previous token of user " + user.getUsername() + ": " + user.getToken());
		userTokens.remove(user.getToken());
		user.updateToken();
		userTokens.put(user.getToken(), user);
		logger.debug("New token of user " + user.getUsername() + ": " + user.getToken());
	}
}
