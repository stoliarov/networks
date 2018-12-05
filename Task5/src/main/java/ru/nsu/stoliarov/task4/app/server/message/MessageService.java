package ru.nsu.stoliarov.task4.app.server.message;

import ru.nsu.stoliarov.task4.app.server.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageService {
	private Map<Long, Message> messages;
	
	public MessageService() {
		messages = new HashMap<>();
	}
	
	public Message addMessage(String text, String author) {
		Message message = new Message(messages.size(), text, author);
		messages.put((long) messages.size(), message);
		return message;
	}
	
	public int messagesCount() {
		return messages.size();
	}
	
	public List<Message> getMessages(int offset, int count) {
		if(offset >= messages.size()) {
			return new ArrayList<Message>();
		} else if(offset + count > messages.size()) {
			count = messages.size() - offset;
		}
		return new ArrayList<Message>(messages.values()).subList(offset, offset + count);
	}
	
	public List<Message> getMessages() {
		return new ArrayList<Message>(messages.values());
	}
}
