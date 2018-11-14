package ru.nsu.stoliarov.task4.app.client;

public enum TaskName {
	LOGIN {
		@Override
		public String toString() {
			return "login";
		}
	},
	LOGOUT {
		@Override
		public String toString() {
			return "logout";
		}
	},
	CONFIRM {
		@Override
		public String toString() {
			return "confirm";
		}
	},
	UPDATE_COUNT {
		@Override
		public String toString() {
			return "update_count";
		}
	},
	SHOW_MESSAGES {
		@Override
		public String toString() {
			return "show_messages";
		}
	},
	SHOW_USERS {
		@Override
		public String toString() {
			return "show_users";
		}
	},
	SHOW_USER {
		@Override
		public String toString() {
			return "show_user";
		}
	},
	WRITE_MESSAGE {
		@Override
		public String toString() {
			return "write_message";
		}
	}
}
