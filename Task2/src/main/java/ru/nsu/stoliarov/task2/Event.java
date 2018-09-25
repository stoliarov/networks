package ru.nsu.stoliarov.task2;

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
	METADATA {
		@Override
		public String toString() {
			return "metadata";
		}
	},
	GOT {
		@Override
		public String toString() {
			return "got";
		}
	},
	DATA {
		@Override
		public String toString() {
			return "data";
		}
	},
	BYE {
		@Override
		public String toString() {
			return "bye";
		}
	}
}
