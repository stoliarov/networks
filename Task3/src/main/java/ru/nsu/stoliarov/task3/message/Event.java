package ru.nsu.stoliarov.task3.message;

/**
 * Describes a type of message a server or a client sends.
 */
public enum Event {
	HI {
		@Override
		public String toString() {
			return "hi";
		}
	},
	MESSAGE {
		@Override
		public String toString() {
			return "message";
		}
	},
	CONFIRM {
		@Override
		public String toString() {
			return "confirm";
		}
	}
}
