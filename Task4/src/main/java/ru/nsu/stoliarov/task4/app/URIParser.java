package ru.nsu.stoliarov.task4.app;

import java.util.ArrayList;
import java.util.List;

public class URIParser {
	public static List<String> pathItems(String path) {
		List<String> pathItems = new ArrayList<>();
		int firstIndex = 0;
		if(0 != path.indexOf('/')) {
			return null;
		}
		
		boolean firstRound = true;
		while(true) {
			int secondIndex = path.indexOf('/', firstIndex + 1);
			if(-1 == secondIndex) {
				String item = path.substring(firstIndex);
				if(!item.equals("/") || firstRound) {
					pathItems.add(item);
				}
				break;
			}
			pathItems.add(path.substring(firstIndex, secondIndex));
			firstIndex = secondIndex;
			firstRound = false;
		}
		return pathItems;
	}
	
	public static String extractPathBeforeNumbers(String path) {
		List<String> pathItems = pathItems(path);
		StringBuilder builder = new StringBuilder();
		if(null == pathItems) {
			return null;
		}
		for(String pathItem : pathItems) {
			if(!isNumeric(pathItem)) {
				builder.append(pathItem);
			} else {
				break;
			}
		}
		return builder.toString();
	}
	
	public static long longValue(String pathItem) {
		return Long.parseLong(pathItem.substring(1));
	}
	
	public static boolean isNumeric(String pathItem) {
		try {
			Long.parseLong(pathItem.substring(1));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
