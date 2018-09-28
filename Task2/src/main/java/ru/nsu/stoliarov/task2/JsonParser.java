package ru.nsu.stoliarov.task2;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonParser {
	static public JSONObject getJSONbyBytes(byte[] bytes, int offset, int length) {
		JSONParser parser = new JSONParser();
		try {
			return (JSONObject) parser.parse(new String(bytes, offset, length));
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static public JSONObject getJSONbyString(String jsonString) {
		JSONParser parser = new JSONParser();
		try {
			return (JSONObject) parser.parse(jsonString);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
}
