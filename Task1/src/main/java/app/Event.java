package app;

/**
 * Describes a type of message app sends.
 */
public enum Event {
	ALIVE {
		@Override
		public String toString() {
			return "alive";
		}
	}
}
