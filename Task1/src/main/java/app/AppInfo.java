package app;

/**
 * Contains the following information about an app instance: ip, port and the time of last activity.
 */
public class AppInfo {
	private String ip;
	private String port;
	private long lastActivityTime;
	
	public AppInfo(String ip, String port, long lastActivityTime) {
		this.ip = ip;
		this.port = port;
		this.lastActivityTime = lastActivityTime;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getPort() {
		return port;
	}
	
	public void setPort(String port) {
		this.port = port;
	}
	
	public long getLastActivityTime() {
		return lastActivityTime;
	}
	
	public void setLastActivityTime(long lastActivityTime) {
		this.lastActivityTime = lastActivityTime;
	}
}
