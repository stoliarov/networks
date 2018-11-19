package ru.nsu.stoliarov.task4.app.client.commands;

import ru.nsu.stoliarov.task4.app.client.CommandParams;
import ru.nsu.stoliarov.task4.app.client.Result;

public abstract class Command {
	public Result execute(CommandParams params) {
		return null;
	}
	
	public void enterData() {
	}
	
	public String getName() {
		return "";
	}
}
