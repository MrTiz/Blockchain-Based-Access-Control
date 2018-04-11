import java.nio.channels.SelectionKey;

public class User {
	private String user;
	private SelectionKey key;
	
	public User(String u, SelectionKey k) {
		this.user = u;
		this.key = k;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public SelectionKey getKey() {
		return this.key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}
}