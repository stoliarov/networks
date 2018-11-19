package ru.nsu.stoliarov.task4.app.client;

import org.apache.http.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HttpResponseParser {
	public static String readBody(HttpResponse response) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
		String output;
		StringBuilder builder = new StringBuilder();
		while((output = reader.readLine()) != null) {
			builder.append(output);
		}
		
		return builder.toString();
	}
}
