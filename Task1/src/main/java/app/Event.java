package app;

/**
 * Describes a type of message app sends.
 */
public enum Event {
	JOIN {
		@Override
		public String toString() {
			return "join";
		}
	},
	ALIVE {
		@Override
		public String toString() {
			return "alive";
		}
	},
	SHOW_LIST {
		@Override
		public String toString() {
			return "show_list";
		}
	}
}
