/**
 * 
 */
package adhoc.voip;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * The Class ConnectionServiceConnection.
 * Performs the activity connection to the ConnectionService instance.
 * @author Raz and Elad
 */
public class ConnectionServiceConnection implements ServiceConnection {
	private static final String LOG_TAG = "ConServiceConnection";
	private MenuActivity menuActivity;
    private ConnectionService connc;
	
	/**
	 * Instantiates a new connection service connection.
	 *
	 * @param menuActivity the menu activity
	 */
	public ConnectionServiceConnection(MenuActivity menuActivity) {
		super();
		Log.v(LOG_TAG,"Constructor");
		this.menuActivity = menuActivity;
	}
	
	/* (non-Javadoc)
	 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
	 */
	@Override
	public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
		Log.v(LOG_TAG,"onServiceConnected");
    //    connc = ((ConnectionService.ConnectionServiceBinder)serviceBinder).getService();
	    menuActivity.setService(((ConnectionService.ConnectionServiceBinder)serviceBinder).getService());
	}

	/* (non-Javadoc)
	 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
	 */
	@Override
	public void onServiceDisconnected(ComponentName name) {
		Log.v(LOG_TAG,"onServiceDisconnected");
		// TODO Auto-generated method stub
	}

}
