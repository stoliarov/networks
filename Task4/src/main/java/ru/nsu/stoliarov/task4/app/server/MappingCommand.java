package ru.nsu.stoliarov.task4.app.server;

import io.undertow.server.HttpServerExchange;

public interface MappingCommand {
	public void execute(HttpServerExchange exchange);
}
