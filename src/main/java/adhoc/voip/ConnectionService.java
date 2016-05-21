
/**
 * 
 */
package adhoc.voip;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * The Class ConnectionService.
 * Responsible of background operations.
 * Maintains the network and data transfer.
 * 
 * Remains active also when the activity is destroyed.
 * Only terminates on active exit.
 *
 * @author Raz and Elad
 */
public class ConnectionService extends Service {
	private static final String LOG_TAG = "ConnectionService";
	private final IBinder binder = new ConnectionServiceBinder();
	private final ConnectActionListener connectActionListener = new ConnectActionListener();;
	//private final CancelConnectActionListener cancelConnectActionListener = new CancelConnectActionListener();
	//private final CreateGroupActionListener createGroupActionListener = new CreateGroupActionListener();
	private final RemoveGroupActionListener removeGroupActionListener = new RemoveGroupActionListener();
	private final DiscoverPeersActionListener discoverPeersActionListener = new DiscoverPeersActionListener();
	private final PeersListListener peersListListener = new PeersListListener();
	private final ConnectionInfoListener connectionInfoListener = new ConnectionInfoListener();

    private MenuActivity activity;
	
	private WifiP2pManager manager;
	private WifiP2pManager.Channel channel;
	private BroadcastReceiver receiver;
	
	private SoundManager soundManager;
    public ConnectionManager connectionManager;

	private boolean wifiP2pEnabled;
	private ConnectionState connectedToGroup;
	
	private Collection<Group> availableDevices;
	private Group currentGroup;
	//private WifiP2pInfo connectionInfo;
	//private WifiP2pDevice wifiP2pDevice;
	private WifiP2pConfig currentConfig;
    private WifiP2pDnsSdServiceInfo serviceInfo;


    private boolean autoConnectToGroup;
	private Group groupToConnect;
	
	private boolean bringActivityToFront;

    /* (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
	@Override
	public void onCreate() {
	    super.onCreate();
		Log.v(LOG_TAG,"onCreate");

		// register broadcast receiver
        receiver = new MenuBroadcastReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(receiver, intentFilter);
	 // get manager and initialize channel
		manager = (WifiP2pManager)getSystemService(WIFI_P2P_SERVICE);
	// 	manager.discoverPeers(channel, discoverPeersActionListener);
	    channel = manager.initialize((Context)this, Looper.getMainLooper(), (WifiP2pManager.ChannelListener)receiver);

	 // get sound manager
        soundManager = new SoundManager(this);
        soundManager.receive();

     // get connection manager
        connectionManager = new ConnectionManager();
        availableDevices = new Vector<Group>();
    	autoConnectToGroup = true;
    	connectedToGroup = ConnectionState.PREEMIE;
    	bringActivityToFront = false;

        startRegistration();

	}

	/* (non-Javadoc)
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.v(LOG_TAG,"onStart");
		// TODO - maybe not needed
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		Log.v(LOG_TAG,"onBind");
		return binder;
	}
	
	
	//===\      /================================================\      /===//
	//====\    /==================================================\    /====//
	//=====\  /===== Interface with the broadcast receiver: =======\  /=====//
	//======\/======================================================\/======//
	
	// called from broadcast-receiver:
	/**
	 *Enables/Disables wifi p2p.
	 *
	 * @param enabled the wifi p2p enabled/disabled boolean
	 */
	public void setWifiP2pEnabled(boolean enabled) {
		Log.v(LOG_TAG,"setWifiP2pEnabled");
		wifiP2pEnabled = enabled;
		if (enabled) {//TODO
			//manager.discoverPeers(channel, discoverPeersActionListener);
			if (bringActivityToFront) {
				bringActivityToFront = false;
	    		//Toast.makeText((Context)this, "You can go back now...", Toast.LENGTH_LONG).show();
				if (activity !=null)
					activity.bringToFront();
			}
		}
		else
			autoConnectToGroup = true;
	}
	
	// called from broadcast-receiver:
	/**
	 * Connect/Disconnect to group.
	 *
	 * @param connected the connect/disconnect boolean
	 */
	public void setConnectedToGroup(boolean connected) {
		Log.v(LOG_TAG,"setConnectedToGroup");
		if (connected)
		{
			soundManager.initiateIP();
			manager.requestConnectionInfo(channel, connectionInfoListener);
			connectedToGroup = ConnectionState.CONNECTED;
		}
		else {
			connectionManager.terminate();
			connectedToGroup = ConnectionState.DISCONNECTED;
		}
		
	}
	
	// called from broadcast-receiver:
	/**
	 * Wifi p2p peers changed.
	 */
	public void wifiP2pPeersChanged() {
		Log.v(LOG_TAG,"wifiP2pPeersChanged");
		manager.requestPeers(channel, peersListListener);
	}
	
	// called from broadcast-receiver:
	/**
	 * Wifi p2p device changed.
	 *
	 * @param device the device
	 */
	public void wifiP2pDeviceChanged(WifiP2pDevice device) {
		Log.v(LOG_TAG,"wifiP2pDeviceChanged");
		//this.wifiP2pDevice = device;
	}
	
	// called from broadcast-receiver:
	/**
	 * Wifi p2p channel disconnected.
	 */
	public void wifiP2pChannelDisconnected() {
		Log.v(LOG_TAG,"wifiP2pChannelDisconnected");
		// TODO - nothing was done in Menu
	}

    private void startRegistration() {

        String deviceId = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("port", String.valueOf(6666));
        record.put("name", deviceId);
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
            }
            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });


    }




	//======/\======================================================/\======//
	//=====/  \====== Interface with the broadcast receiver =======/  \=====//
	//====/    \==================================================/    \====//
	//===/      \================================================/      \===//



    //===\      /================================================\      /===//
    //====\    /==================================================\    /====//
    //=====\  /===== Interface with the bluetooth: ================\  /=====//
    //======\/======================================================\/======//


    public List<BluetoothDevice> showBondedDevice(){
        return soundManager.showBondedDevice();
    }

    public void connectToBluetooth(BluetoothDevice device){
        soundManager.connectToBluetooth(device);
    }

    public void setmHandler(Handler handler){
        soundManager.setmHandler(handler);
        Log.v("CS層", "mHandler=" + handler);
    }

    //======/\======================================================/\======//
    //=====/  \====== Interface with the bluetooth ================/  \=====//
    //====/    \==================================================/    \====//
    //===/      \================================================/      \===//


	
	//===\      /================================================\      /===//
	//====\    /==================================================\    /====//
	//=====\  /===== Interface with the main activity: ============\  /=====//
	//======\/======================================================\/======//
	
	/**
	 * Sets the related activity.
	 *
	 * @param activity the new activity
	 */
	public void setActivity(MenuActivity activity) {
		Log.v(LOG_TAG,"setActivity");
		this.activity = activity;
	}
	

	/**
	 * Connect to a group.
	 *
	 * @param group the group
	 */
	public void connectToGroup(Group group) {
		Log.v(LOG_TAG,"connectToGroup");
		currentConfig = new WifiP2pConfig();
		currentConfig.deviceAddress = group.getOwnerAddress();
		currentConfig.wps.setup = WpsInfo.PBC;
		connectActionListener.setGroup(group);
		manager.connect(channel, currentConfig, connectActionListener);
		connectedToGroup = ConnectionState.PENDING;
	}
	/**
	 * Cancel connect.
	 */
	public void cancelConnect() { // TODO - something that really helps...
		Log.v(LOG_TAG,"cancelConnect");
		autoConnectToGroup = true;
		//manager.cancelConnect(channel, cancelConnectActionListener);
		manager.removeGroup(channel, removeGroupActionListener);
		connectionManager.terminate();
	}
	
	/**
	 * Talk.
	 */
	public void talk() {
		Log.v(LOG_TAG,"talk");
		soundManager.sendAudio();
	}

	/**
	 * Hang up.
	 */
	public void hangUp() {
		Log.v(LOG_TAG,"hangUp");
		soundManager.stopRecording();
	}

    /**
     * say.
     */
    public void SAY() {
        Log.v(LOG_TAG,"say");
        soundManager.SAY();
    }

    /**
     * stopsay.
     */
    public void stopSAY() {
        Log.v(LOG_TAG,"say");
        soundManager.stopSAY();
    }

	
	/**
	 * Refresh.
	 * Scans the network finding possible members.
	 */
	public void refresh() {
		Log.v(LOG_TAG,"refresh");
		manager.discoverPeers(channel, discoverPeersActionListener);
	}
	
	/**
	 * Checks if wifi p2p is enabled.
	 *
	 * @return true, if wifi p2p is enabled
	 */
	public boolean isWifiP2pEnabled() {
		//Log.v(LOG_TAG,"isWifiP2pEnabled");
		return wifiP2pEnabled;
	}
	
	/**
	 * Checks the connection to group state.
	 *
	 * @return the connection state
	 */
	public ConnectionState isConnectedToGroup() {
		//Log.v(LOG_TAG,"isConnectedToGroup");
		return connectedToGroup;
	}
	
	/**
	 * Gets the available devices.
	 *
	 * @return the available devices
	 */
	public Collection<Group> getAvailableDevices() {
		//Log.v(LOG_TAG,"getAvailableGroups");
		// keep updating:
		//manager.requestPeers(channel, peersChangedPeersListListener); TODO
		return availableDevices;
	}

	/**
	 * Gets the group members.
	 *
	 * @return the group members
	 */
	public Collection<Member> getGroupMembers() {
		//Log.v(LOG_TAG,"getGroupMembers");
		Collection<Member> dummy = new ArrayList<Member>(connectionManager.getGroupMembers());
		return dummy;
	}

	/**
	 * Sets the bring-activity-to-front configuration.
	 *
	 * @param bringToFront the new bring-activity-to-front configuration
	 */
	public void setBringActivityToFront(boolean bringToFront) {
		this.bringActivityToFront = bringToFront;
	}

	/**
	 * Terminate.
	 * Disable wifi direct.
	 * Close sound manager
	 * Save application data to file.
	 * Stop service.
	 */
	public void terminate() {
		Log.v(LOG_TAG,"terminate");
		autoConnectToGroup = false;
		if (connectedToGroup.equals(ConnectionState.CONNECTED) || connectedToGroup.equals(ConnectionState.PENDING))
			manager.removeGroup(channel, removeGroupActionListener);
		
		unregisterReceiver(receiver);
		soundManager.shutDown();

		// TODO (close sockets and kill threads)
		// disconnect from wifi direct
		try {
			Class cls = Class.forName("android.net.wifi.p2p.WifiP2pManager");
			Class partypes[] = new Class[1];
			partypes[0] = Channel.class;
			Method meth = cls.getMethod("disableP2p", partypes);
			Object arglist[] = new Object[1];
			arglist[0] = channel;
			meth.invoke(manager, arglist);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stopSelf();
	}
	
	//======/\======================================================/\======//
	//=====/  \====== Interface with the main activity ============/  \=====//
	//====/    \==================================================/    \====//
	//===/      \================================================/      \===//
	
	
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Log.v(LOG_TAG,"onDestroy");	
		// TODO - remove
		super.onDestroy();
	}
	
	// Action listeners:
	
	private class ConnectActionListener implements WifiP2pManager.ActionListener {
		private static final String LOG_TAG = "ConnectActionListener";
		private int temptsCounter;
		private Group groupToConnect;
		
		public ConnectActionListener() {
			Log.v(LOG_TAG,"Constructor");
			temptsCounter = 0;
		}
	
		
		public void setGroup(Group groupToConnect) {
			Log.v(LOG_TAG,"setGroup");
			this.groupToConnect = groupToConnect;
		}
		
		@Override
		public void onFailure(int reason) {
			Log.v(LOG_TAG,"onFailure");
			temptsCounter++;
			if (temptsCounter <= 10)
				// keep trying
				new Thread(new TryAgain()).start();
			else
				temptsCounter = 0;
		}

		@Override
		public void onSuccess() {
			Log.v(LOG_TAG,"onSuccess");
			temptsCounter = 0;
			currentGroup = groupToConnect;
		}
		
		private class TryAgain implements Runnable {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
                    manager.connect(channel,currentConfig,ConnectActionListener.this);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	private class CancelConnectActionListener implements WifiP2pManager.ActionListener {
		private static final String LOG_TAG = "CancelConnectActionListener";

		@Override
		public void onFailure(int reason) {
			Log.v(LOG_TAG,"onFailure");
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSuccess() {
			Log.v(LOG_TAG,"onSuccess");
			connectedToGroup = ConnectionState.DISCONNECTED;
		}
	}
	*/
	
	/*
	private class CreateGroupActionListener implements WifiP2pManager.ActionListener {
		private static final String LOG_TAG = "CreateGroupActionListener";
		private int temptsCounter;
		
		public CreateGroupActionListener() {
			Log.v(LOG_TAG,"Constructor");
			temptsCounter = 0;
		}
		
		@Override
		public void onFailure(int reason) {
			Log.v(LOG_TAG,"onFailure");
			temptsCounter++;
			if (temptsCounter <= 10)
				// keep trying
				new Thread(new TryAgain()).start();
			else
				temptsCounter = 0;
			
		}

		@Override
		public void onSuccess() {
			Log.v(LOG_TAG,"onSuccess");
			temptsCounter = 0;			
		}
		
		private class TryAgain implements Runnable {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
					//manager.createGroup(channel,CreateGroupActionListener.this);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	*/


	private class RemoveGroupActionListener implements WifiP2pManager.ActionListener {
		private static final String LOG_TAG = "RemoveGroupListener";

		@Override
		public void onFailure(int reason) {
			Log.v(LOG_TAG,"onFailure");
			// TODO Auto-generated method stub
		}

		@Override
		public void onSuccess() {
			Log.v(LOG_TAG,"onSuccess");
			// TODO Auto-generated method stub
		}

	}
	
	private class DiscoverPeersActionListener implements WifiP2pManager.ActionListener {
		private static final String LOG_TAG = "DiscoverPeersListener";
		private int temptsCounter;
		
		public DiscoverPeersActionListener() {
			Log.v(LOG_TAG,"Constructor");
			temptsCounter = 0;
		}
		
		@Override
		public void onFailure(int reason) {
			Log.v(LOG_TAG,"onFailure");
			temptsCounter++;
			if (temptsCounter <= 10)
				// keep trying
				new Thread(new TryAgain()).start();
			else
				temptsCounter = 0;
		}

		@Override
		public void onSuccess() {
			Log.v(LOG_TAG,"onSuccess");
			temptsCounter = 0;
		}
		
		private class TryAgain implements Runnable {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
					manager.discoverPeers(channel,DiscoverPeersActionListener.this);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private class PeersListListener implements WifiP2pManager.PeerListListener {
		private static final String LOG_TAG = "PeerChangedListListener";
		private boolean found = false;

		@Override
		public void onPeersAvailable(WifiP2pDeviceList peers) {
			Log.v(LOG_TAG,"onPeersAvailable");
			synchronized(availableDevices) {
                    availableDevices.clear();
                    for (WifiP2pDevice device : peers.getDeviceList()) {
					groupToConnect = null;
					availableDevices.add(new Group(false,device,null));
					if((!isConnectedToGroup().equals(ConnectionState.CONNECTED)) && !found && autoConnectToGroup) {
						groupToConnect = new Group(false,device,null);
						if (device.isGroupOwner()) {
							found = true;
						}
					}
				}
				if(wifiP2pEnabled)
					autoConnectToGroup = false;
			}

			/*
			if((connectedToGroup.equals(ConnectionState.DISCONNECTED) || connectedToGroup.equals(ConnectionState.PREEMIE)) && groupToConnect != null) {
				//connectToGroup(groupToConnect);
			}*/
		}
	}
	
	private class ConnectionInfoListener implements WifiP2pManager.ConnectionInfoListener {
		@Override
		public void onConnectionInfoAvailable(WifiP2pInfo info) {
			//connectionInfo = info;
			connectionManager.initiate(info); // TODO - change name
		}
	}


	
	/**
	 * The Class ConnectionServiceBinder.
	 * Binds the service to the activity.
	 */
	public class ConnectionServiceBinder extends Binder {
		private static final String LOG_TAG = "ConnectionServiceBinder";
		/**
		 * Gets the service.
		 *
		 * @return the service
		 */
		public ConnectionService getService() {
			Log.v(LOG_TAG,"getService");
			return ConnectionService.this;
		}
	}
	
	/**
	 * The Enum ConnectionState.
	 * Represents the connection state.
	 */
	public enum ConnectionState {
		PREEMIE,            // service has just created.
        DISCONNECTED,       // not connected to group.
        CONNECTED,          // connected to group. 
        PENDING             // trying to connect to a group.
	}

}

