/**
 * 
 */
package adhoc.voip;

import java.net.InetAddress;
import java.net.Socket;

/**
 * The Class Member.
 * Represent a single network member.
 *
 * @author Raz and Elad
 */
public class Member {
	private final int INITIAL_COUNTDOWN = 5;
	private boolean active;
	private int deathCountdown;
	private String name;
	private Socket socket;
	private InetAddress IP;
	
	/**
	 * Instantiates a new member.
	 */
	public Member() {
		this.active = true;
		this.deathCountdown = INITIAL_COUNTDOWN;
		this.name = null;
		this.socket = null;
		this.IP = null;
	}
	
	/**
	 * Instantiates a new member.
	 *
	 * @param name the name
	 * @param IP the IP address
	 */
	public Member(String name, InetAddress IP) {
		this.active = true;
		this.deathCountdown = INITIAL_COUNTDOWN;
		this.name = name;
		this.socket = null;
		this.IP = IP;
	}
	
	/**
	 * Instantiates a new member.
	 *
	 * @param name the name
	 * @param socket the connection socket
	 */
	public Member(String name, Socket socket) {
		this.active = true;
		this.deathCountdown = INITIAL_COUNTDOWN;
		this.name = name;
		this.socket = socket;
		this.IP = socket.getInetAddress();
	}
	
	/**
	 * Gets the member's name.
	 *
	 * @return the member's name
	 */
	public String getName() { 
		return name; 
	}
	
	/**
	 * Sets the member's name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) { 
		this.name = name; 
	}
	
	/**
	 * Gets the IP address.
	 *
	 * @return the IP
	 */
	public InetAddress getIP() {
		return IP;
	}
	
	/**
	 * Sets the IP address.
	 *
	 * @param IP the new IP
	 */
	public void setIP(InetAddress IP) {
		this.IP = IP;
	}
	
	/**
	 * Gets the connection socket.
	 *
	 * @return the socket
	 */
	public Socket getSocket() { 
		return socket; 
	}
	
	/**
	 * Sets the connection socket.
	 *
	 * @param socket the new socket
	 */
	public void setSocket(Socket socket) { 
		this.socket = socket;
		this.IP = socket.getInetAddress();
	}
	
	/**
	 * Checks if is active.
	 *
	 * @return true, if is active
	 */
	public boolean isActive() { 
		return active; 
	}
	
	/**
	 * Sets the active boolean.
	 *
	 * @param active the new active value
	 */
	public void setActive(boolean active) { 
		this.active = active; 
	}
	
	/**
	 * Checks if is alive.
	 *
	 * @return true, if is alive
	 */
	public boolean isAlive() { 
		return (deathCountdown > 0); 
	}
	
	/**
	 * Decrement death countdown.
	 * (when gets to 0 means its dead)
	 */
	public void decDeathCountdown() { 
		if (isAlive()) deathCountdown--; 
	}
	
	/**
	 * Reset death countdown.
	 */
	public void resetDeathCountdown() { 
		deathCountdown = INITIAL_COUNTDOWN; 
	}
}
