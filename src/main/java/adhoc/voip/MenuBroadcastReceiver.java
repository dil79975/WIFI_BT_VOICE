/*
 * 
 */
package adhoc.voip;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

// TODO: Auto-generated Javadoc
/**
 * The Class MenuBroadcastReceiver.
 * Receives intents regards the wifi-p2p connection and act upon them.
 * 
 * @author Raz and Elad
 */
public class MenuBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.ChannelListener {
	private static final String LOG_TAG = "MenuBroadcastReceiver";

    /** The menu activity. */
    private ConnectionService service;

    /**
     * Instantiates a new menu broadcast receiver.
     *
     * @param service the connection service
     */
    public MenuBroadcastReceiver(ConnectionService service) {
        super();
		Log.v(LOG_TAG,"Constractor");
        this.service = service;
    }

    /**
     * On recieve (intent).
     *
     * @param context the context
     * @param intent the intent
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
		Log.v(LOG_TAG,"onReceive - " + action);
        
        // wifi-p2p state changed
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        	if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
        		service.setWifiP2pEnabled(true);
        	else
        		service.setWifiP2pEnabled(false);
        } 
        
        // wifi-p2p peers changed
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action))
         	service.wifiP2pPeersChanged();
        
        
        // wifi-p2p connection changed
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected())
            	service.setConnectedToGroup(true);
            else
            	service.setConnectedToGroup(false);
        } 

        // wifi-p2p device changed
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        	service.wifiP2pDeviceChanged(device);
        }


        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            Log.v("searchDevices","Search Complete Device");
            // 當收尋到裝置時
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                // 取得藍芽裝置這個物件
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }

        }


    }

	/* (non-Javadoc)
	 * @see android.net.wifi.p2p.WifiP2pManager.ChannelListener#onChannelDisconnected()
	 */
	@Override
	public void onChannelDisconnected() {
		Log.v(LOG_TAG,"onChannelDisconnected");
		service.wifiP2pChannelDisconnected();	
	}
}
