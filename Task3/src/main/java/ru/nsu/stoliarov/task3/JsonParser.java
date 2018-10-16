package ru.nsu.stoliarov.task3;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonParser {
	static public JSONObject getJSONbyBytes(byte[] bytes, int offset, int length) {
		JSONParser parser = new JSONParser();
		String string = new String(bytes, offset, length);
		string = string.trim();
		try {
			return (JSONObject) parser.parse(string);
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println(string);
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
