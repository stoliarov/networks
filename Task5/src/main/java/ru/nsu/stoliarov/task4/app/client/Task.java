package ru.nsu.stoliarov.task4.app.client;

import ru.nsu.stoliarov.task4.app.client.commands.Command;

public class Task {
	private Command command;
	private CommandParams params;
	private boolean isUserTask;
	
	public Task(Command command, CommandParams params, boolean isUserTask) {
		this.command = command;
		this.isUserTask = isUserTask;
		this.params = params;
	}
	
	public boolean isUserTask() {
		return isUserTask;
	}
	
	public Command getCommand() {
		return command;
	}
	
	public void setCommand(Command command) {
		this.command = command;
	}
	
	public CommandParams getParams() {
		return params;
	}
	
	public void setParams(CommandParams params) {
		this.params = params;
	}
}
