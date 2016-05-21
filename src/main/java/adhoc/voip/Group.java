/**
 * 
 */
package adhoc.voip;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import java.util.HashMap;

/**
 * The Class Group.
 * Represents a users group
 * 
 * @author Raz and Elad
 */
public class Group {
	private static final String LOG_TAG = "Group";
	
	private static HashMap<String,String> addressToGroupNameMap = new HashMap<String, String>();
	
	private boolean isGroupOwner;
	private WifiP2pDevice owner;
	private String name;
	
	/**
	 * Instantiates a new group.
	 *
	 * @param isGroupOwner true for group owner
	 * @param owner the owner
	 * @param name the name
	 */
	public Group(boolean isGroupOwner, WifiP2pDevice owner, String name) {
		Log.v(LOG_TAG,"Constructor");
		this.isGroupOwner = isGroupOwner;
		this.owner = owner;
		if (name == null)
			name = addressToGroupNameMap.get(owner.deviceAddress);
		if (name == null)
			name = owner.deviceName;
		this.name = name;
	}
	
	/**
	 * Checks if is group owner.
	 *
	 * @return true, if is group owner
	 */
	public boolean isGroupOwner() {
		return isGroupOwner;
	}
	
	/**
	 * Gets the owners address.
	 *
	 * @return the owner address
	 */
	public String getOwnerAddress (){
		Log.v(LOG_TAG,"getOwnerAddress");
		return owner.deviceAddress;
	}
	
	/**
	 * Sets the group name.
	 *
	 * @param name the new group name
	 */
	public void setName(String name) {
		Log.v(LOG_TAG,"setName");
		this.name = name;
		addressToGroupNameMap.put(owner.deviceAddress, name);
	}
	
	/**
	 * Gets the group name.
	 *
	 * @return the group name
	 */
	public String getName()  {
	//	Log.v(LOG_TAG,"getName");
		return name;
	}
	
	/**
	 * Gets the address-to-group-name map.
	 *
	 * @return the address-to-group-name map
	 */
	public static HashMap<String, String> getAddressToGroupNameMap() {
		Log.v(LOG_TAG,"getAddressToGroupNameMap");
		return addressToGroupNameMap;
	}
}
