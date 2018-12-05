package ru.nsu.stoliarov.task4.app;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Properties;

public class PropertiesConfigurator {
	private static final Logger logger = LogManager.getLogger(PropertiesConfigurator.class.getName());
	
	public static Properties getProperties(String pathToProperties) {
		Properties properties = new java.util.Properties();
		try {
			Class classObj = Class.forName(PropertiesConfigurator.class.getName());
			ClassLoader loader = classObj.getClassLoader();
			
			java.io.InputStream inStream = loader.getResourceAsStream(pathToProperties);
			if(null == inStream) {
				logger.error("Not found this properties (wrong path): " + pathToProperties);
				return null;
			}
			properties.load(inStream);
			return properties;
		} catch (ClassNotFoundException exception) {
			logger.error("Class not found: " + PropertiesConfigurator.class.getName());
			return null;
		} catch (java.io.IOException exception) {
			logger.error("File with properties not found: " + pathToProperties);
			return null;
		}
	}
}
