package ru.nsu.stoliarov.task4.app.client;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.nsu.stoliarov.task4.app.PropertiesConfigurator;
import ru.nsu.stoliarov.task4.app.client.commands.Command;

import javax.xml.datatype.DatatypeConfigurationException;
import java.lang.reflect.InvocationTargetException;

public class Factory {
	private static final String PATH_TO_PROPERTIES = "factoryConfig.properties";
	
	private static final Logger logger = LogManager.getLogger(Factory.class.getName());
	
	private static Factory instance = null;
	private static java.util.Map<String, String> commands = new java.util.HashMap<>();
	
	private Factory() {
	}
	
	public static Factory getInstance() {
		if(null == instance) {
			instance = new Factory();
			java.util.Properties properties = PropertiesConfigurator.getProperties(PATH_TO_PROPERTIES);
			if(null != properties) {
				setConfig(properties);
				logger.debug("Factory successfully created");
			} else {
				logger.warn("Factory didn't get properties");
			}
		}
		return instance;
	}
	
	public Command getCommand(String commandName) throws DatatypeConfigurationException,
			InstantiationException, IllegalAccessException, ClassNotFoundException {
		
		if(commands.isEmpty()) {
			logger.error("Configuration of factory doesn't set");
			throw new DatatypeConfigurationException("Configuration not specified");
		}
		
		if(commands.containsKey(commandName)) {
			try {
				Class commandClass = Class.forName(commands.get(commandName));
				try {
					try {
						Object object = commandClass.getConstructor().newInstance();
						return (Command) object;
						
					} catch (NoSuchMethodException | InvocationTargetException e) {
						logger.error("Failed to create command: " + commandName);
						e.printStackTrace();
					}
				} catch (IllegalAccessException exception) {
					logger.error("Failed to create command: " + commandName + ". Access error");
					throw exception;
				}
			} catch (ClassNotFoundException exception) {
				logger.error("Class matching to command '" + commandName + "' not found");
				throw exception;
			}
		} else {
			logger.error("Command '" + commandName + "' not specified in configuration");
			throw new DatatypeConfigurationException("Not found: '" + commandName + "'");
		}
		
		return null;
	}
	
	static private void setConfig(java.util.Properties properties) {
		java.util.Set<String> nameCommandsSet = properties.stringPropertyNames();
		java.util.Iterator<String> iterator = nameCommandsSet.iterator();
		while(iterator.hasNext()) {
			String commandName = iterator.next();
			String className = properties.getProperty(commandName);
			commands.put(commandName, className);
		}
		logger.debug("Factory got a properties");
	}
}
