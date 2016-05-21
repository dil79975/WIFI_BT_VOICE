/*
 * 
 */
package adhoc.voip;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

// TODO: Auto-generated Javadoc
/**
 * The Class MenuActivity.
 * Responsible for the User Interface.
 * 
 * @author Raz and Elad
 */
public class MenuActivity extends Activity {
	private static final String LOG_TAG = "MenuActivity";
    private String mConnectedDeviceName;
	private final long TIMER_DELAY = 500;
	private final long TIMER_PERIOD = 2500;

	//perhaps delete
	//private boolean terminated;
	
	private ServiceConnection serviceConnection;
	private ConnectionService service;
	
	private Timer timer;
	
	private PopupsManager popupsManager;
	
	private ScrollView scrollView;
	private GridLayout groupsGridLayout;
	private GridLayout membersGridLayout;
    private LinearLayout bluetoothLinearLayout;
	private LinearLayout tabsLayout;
    private LinearLayout deviceInfoLayout;
    private LinearLayout test_area;
	private View groupsTab;
	private View membersTab;
    private View bluetoothDevicesTab;
    private ToggleButton speakerPhoneOn;

	private HashMap<View,Group> viewToGroupMap;
	private Group pressedGroup;
	private TextView groupNameTextView;
	
	private HashMap<View,Member> viewToMemberMap;
	//private InetAddress IP;
	
	private enum OfflineMode {UNKNOWN,ONLINE,OFFLINE}
	private OfflineMode offlineMode;
    private Set<BluetoothDevice> ssDevice;
    private List<BluetoothDevice> device = new ArrayList<BluetoothDevice>();
    private BluetoothAdapter mBluetoothAdapter;
    private TextView say_status,MY_IP,connetedBT;
    private ImageButton SAY_BUTTON;
    private boolean Saying=false;

    public  Time t = new Time(Time.getCurrentTimezone());
    private AudioManager audioManager;
    public ConnectionManager cManager;


    /* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(LOG_TAG, "onCreate");

		setContentView(R.layout.main_layout);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		this.serviceConnection = new ConnectionServiceConnection((MenuActivity)this);
    	startService(new Intent((Context) this, ConnectionService.class));
    	bindService(new Intent((Context)this, ConnectionService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
    	
    	this.popupsManager = new PopupsManager((Context)this);

    	this.viewToGroupMap = new HashMap<View, Group>();
    	this.groupNameTextView = (TextView) findViewById(R.id.current_group_name);
    	
    	this.viewToMemberMap = new HashMap<View, Member>();

    	LayoutInflater li = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.groupsGridLayout = (GridLayout) li.inflate(R.layout.groups_grid_layout, null,false);
		this.membersGridLayout = (GridLayout) li.inflate(R.layout.members_grid_layout, null,false);
        this.bluetoothLinearLayout = (LinearLayout) li.inflate(R.layout.bluetooth_liner_layout, null,false);
        this.deviceInfoLayout = (LinearLayout) li.inflate(R.layout.deviceinfo_layout,null,false);
        this.test_area = (LinearLayout) li.inflate(R.layout.test_area, null,false);

		this.groupsTab = li.inflate(R.layout.transperent_line, null,false);
		this.membersTab = li.inflate(R.layout.light_blue_line, null,false);
        this.bluetoothDevicesTab = li.inflate(R.layout.third_line, null,false);
    	this.scrollView = (ScrollView) findViewById(R.id.scroll_view);
		this.scrollView.addView(membersGridLayout);
		this.tabsLayout = (LinearLayout) findViewById(R.id.tabsLayout) ;

        this.say_status = (TextView) popupsManager.talkDialog.findViewById(R.id.say_status);
        this.SAY_BUTTON = (ImageButton) popupsManager.talkDialog.findViewById(R.id.SAY_BUTTON);
        this.MY_IP = (TextView) findViewById(R.id.MY_IP);
        this.connetedBT = (TextView) findViewById(R.id.connectedBTDevice);
        this.speakerPhoneOn = (ToggleButton) popupsManager.talkDialog.findViewById(R.id.speakerPhoneOn);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        this.offlineMode = OfflineMode.UNKNOWN;

        SAY_BUTTON.setOnClickListener(say);
        speakerPhoneOn.setOnClickListener(speakerOn);
	}   
    
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.v(LOG_TAG,"onResume");
		this.timer = new Timer();
        this.timer.schedule(new MenuActivityTimerTask(), TIMER_DELAY, TIMER_PERIOD);
	}
	
	/**
	 * Sets the service.
	 *
	 * @param service the new service
	 */
	public void setService(ConnectionService service) {
        Log.v(LOG_TAG, "setService");
		this.service = service;
        service.setActivity((MenuActivity) this);
        service.setmHandler(mHandler);
	}
	
	/*

    public void settings(View v) {
		Log.v(LOG_TAG,"settings");
		this.popupsManager.settingsPop.showAsDropDown(v);
    }
    */

    public void BluetoothActivity(View v){
        Intent intent = new Intent();
        intent.setClass(MenuActivity.this, BluetoothActivity.class);
        startActivity(intent);
    }
	
	/**
	 * Bring activity to front.
	 */
	public void bringToFront() {
		Log.v(LOG_TAG,"bringToFront");
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
	    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		ComponentName cn = new ComponentName(this, MenuActivity.class);
		intent.setComponent(cn);
		startActivity(intent);
	}
    
    /**
     * Wifi p2p settings. ("onClick")
     *
     * @param v Wifi p2p settings button
     */
    public void wifiP2pSettings(View v) {
    	//popupsManager.settingsPop.dismiss();
    	popupsManager.enableWifiDialog.dismiss();
    	if (offlineMode.equals(OfflineMode.UNKNOWN))
			offlineMode = OfflineMode.ONLINE;
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    	if (!service.isWifiP2pEnabled())
    		Toast.makeText((Context)this, "Please enable wifi direct...", Toast.LENGTH_LONG).show();

    }
    
    /**
     * Shut-Down application. ("onClick")
     *
     * @param v Shut-Down button
     */
    public void shutDown(View v) {
		Log.v(LOG_TAG,"shutDown");
    	//popupsManager.settingsPop.dismiss();
		//terminated = true;
		service.terminate();
    	finish();
    }


    /**
     * Shut-Down application. ("onClick")
     *
     * @param v drop button
     */
    public void drop(View v) {
        service.cancelConnect();
    }

    /**
     * Shut-Down application. ("onClick")
     *
     * @param v discover button
     */

    public void discover(View v) {
        service.refresh();
    }

    
    /**
     * Work offline.
     *
     * @param v the work offline button
     */
    public void workOffline(View v) {
    	Log.v(LOG_TAG,"workOffline");
    	offlineMode = OfflineMode.OFFLINE;
        service.setBringActivityToFront(false);
    	popupsManager.enableWifiDialog.dismiss();
    }

    /**
     * Talk button "onClick".
     *
     * @param v Talk button
     */
    public void talk(View v) {
		Log.v(LOG_TAG,"talk");
        popupsManager.talkDialog.show();
        service.talk();
    }
    
    /**
     * Hang-Up button "onClick".
     *
     * @param v Hang-Up button
     */
    public void hangUp(View v) {
		Log.v(LOG_TAG,"hangUp");
		if (service == null)
			return;
		popupsManager.talkDialog.dismiss();
		service.hangUp();
    }


    private View.OnClickListener speakerOn = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(speakerPhoneOn.isChecked()){
                audioManager.setSpeakerphoneOn(true);
                Toast.makeText(getApplicationContext(),"擴音開啟" + audioManager.isSpeakerphoneOn()
                                                                 , Toast.LENGTH_SHORT).show();
            }
            else {
                audioManager.setSpeakerphoneOn(false);
                Toast.makeText(getApplicationContext(), "擴音關閉" + audioManager.isSpeakerphoneOn()
                                                                  , Toast.LENGTH_SHORT).show();
            }


        }
    };

    private View.OnClickListener say = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if (service == null)
                return;
            if(!Saying) {
          //      showSendTime();
                say_status.setText("正在說話....");
                service.SAY();
                Saying=true;
            }
            else if(Saying){
          //      showReceiveTime();
                say_status.setText("未說話");
                service.stopSAY();
                Saying=false;
            }
        }
    };

	/**
	 * Group click.
	 * Connects to group
	 *
	 * @param groupView the group view
	 */
	public void group(View groupView) {
		Log.v(LOG_TAG, "group");
		// TODO
		pressedGroup = viewToGroupMap.get(groupView);
		if (service == null)
			return;
        service.connectToGroup(pressedGroup);
	}

	/**
	 * Show available groups tab.
	 *
	 * @param v the available groups tab
	 */
	public void availiableGroups(View v) {
		Log.v(LOG_TAG,"show availiable groups");
		this.scrollView.removeAllViews();
		this.scrollView.addView(groupsGridLayout);
		this.tabsLayout.removeAllViews();
		this.tabsLayout.addView(groupsTab);
	}
	
	/**
	 * Show group members tab.
	 *
	 * @param v the group members tab
	 */
	public void groupMember(View v) {
        Log.v(LOG_TAG,"show group's members");
        this.scrollView.removeAllViews();
        this.scrollView.addView(membersGridLayout);
        this.tabsLayout.removeAllViews();
        this.tabsLayout.addView(membersTab);
	}


    public void deviceInfo(View v){
        Log.v(LOG_TAG, "show device info");
        this.scrollView.removeAllViews();
        this.scrollView.addView(deviceInfoLayout);
        this.tabsLayout.removeAllViews();

        TextView t1 =(TextView) findViewById(R.id.T1);
        TextView t2 =(TextView) findViewById(R.id.T2);
        TextView t3 =(TextView) findViewById(R.id.T3);
        TextView t4 =(TextView) findViewById(R.id.T4);
        TextView t5 =(TextView) findViewById(R.id.T5);
        TextView t6 =(TextView) findViewById(R.id.T6);
        TextView t7 =(TextView) findViewById(R.id.T7);
        TextView t8 =(TextView) findViewById(R.id.T8);
        TextView t9 =(TextView) findViewById(R.id.T9);
        TextView t10 =(TextView) findViewById(R.id.T10);
        TextView t11 =(TextView) findViewById(R.id.T11);
        TextView t12 =(TextView) findViewById(R.id.T12);
        TextView t13 =(TextView) findViewById(R.id.T13);
        TextView t14 =(TextView) findViewById(R.id.T14);

        t1.setText("主機版:"+Build.BOARD);
        t2.setText("品牌名稱:"+Build.BRAND);
        t3.setText("CPU:"+Build.CPU_ABI);
        t4.setText("設備名稱:"+Build.DEVICE);
        t5.setText("版本號碼:"+Build.DISPLAY);
        t6.setText("設備識別碼:"+Build.FINGERPRINT);
        t7.setText("HOST:"+Build.HOST);
        t8.setText("版本號碼:"+Build.ID);
        t9.setText("製造商:"+Build.MANUFACTURER);
        t10.setText("模組號碼:"+Build.MODEL);
        t11.setText("產品名稱:"+Build.PRODUCT);
        t12.setText("設備描述:"+Build.TAGS);
        t13.setText("設備類別:"+Build.TYPE);
        t14.setText("USER:"+Build.USER);

    }

    public void testArea(View v){
        cManager = service.connectionManager;
        Log.v(LOG_TAG, "testArea");
        this.scrollView.removeAllViews();
        this.scrollView.addView(test_area);
    }

    public void test1(View v){
        Log.v(LOG_TAG, "showMember");
        Collection<Member> members = service.getGroupMembers();
        for(Member member:members) {
            Log.v(LOG_TAG, "member name:"+member.getName());
            Log.v(LOG_TAG, "member IP:"+member.getIP());
        }
    }
    public void test2(View v) throws IOException {
        Log.v(LOG_TAG, "test2");
        cManager.CLIENT_TO_GO();
    }
    public void test3(View v) throws IOException {
        Log.v(LOG_TAG, "test3");
        cManager.GO_TO_CLIENT();
    }
    public void test4(View v){
        Log.v(LOG_TAG, "test4");
    }
    public void test5(View v){
        Log.v(LOG_TAG, "test5");
    }
    public void test6(View v){
        Log.v(LOG_TAG, "test6");
    }
    public void test7(View v){
        Log.v(LOG_TAG, "test7");
    }
    public void test8(View v){
        Log.v(LOG_TAG, "test8");
    }
    public void test9(View v){
        Log.v(LOG_TAG, "test9");
    }


    /**
     * Show group members tab.
     *
     * @param v the group members tab
     */
    public void bluetoothDevice(View v) {
        Log.v(LOG_TAG, "show bluetoothDevice");
        this.scrollView.removeAllViews();
        this.scrollView.addView(bluetoothLinearLayout);
        this.tabsLayout.removeAllViews();
        this.tabsLayout.addView(bluetoothDevicesTab);
        updateBluetoothDevice(v);
    }

    public void updateBluetoothDevice(View v){

        LayoutInflater li = (LayoutInflater) MenuActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        bluetoothLinearLayout.removeAllViews();
        device.clear();
        ssDevice = mBluetoothAdapter.getBondedDevices();
        device = service.showBondedDevice();
        for(int i=0 ; i<ssDevice.size() ; i++) {
            LinearLayout layout = (LinearLayout) li.inflate(R.layout.bluetoothdevice, null, false);
            final TextView btd = ((TextView) layout.findViewById(R.id.btdevice_text));
            btd.setText(device.get(i).getName() + "\n" + device.get(i).getAddress());
            bluetoothLinearLayout.addView(layout);

            btd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < ssDevice.size(); i++) {
                        String A, B;
                        A = btd.getText().toString();
                        B = device.get(i).getName() + "\n" + device.get(i).getAddress().toString();
                        if (A.equals(B)) {
                            Log.v("AAA", "比對成功" + i);
                            service.connectToBluetooth(device.get(i));
                            Toast.makeText(getApplicationContext(), "Connect to " + device.get(i), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            });
            }
        }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg){
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case SoundManager.STATE_CONNECTED:
                            Log.v("111","STATE_CONNECTED");
                            break;
                        case SoundManager.STATE_CONNECTING:
                            Log.v("111","STATE_CONNECTING");
                            break;
                        case SoundManager.STATE_LISTEN:
                            Log.v("111","STATE_LISTEN");
                        case SoundManager.STATE_NONE:
                            Log.v("111","STATE_NONE");
                            break;
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    connetedBT.setText("藍芽裝置:"+mConnectedDeviceName);
                    break;
                case Constants.MESSAGE_TOAST:
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    connetedBT.setText("藍芽裝置:未連接");
                    break;

                case Constants.MESSAGE_MYIP:
                    String myip = msg.getData().getString("myIP");
                    MY_IP.setText("WIFI IP:"+myip);
            }
        }
    };



	@Override
	protected void onPause() {
		Log.v(LOG_TAG,"onPause");
		this.timer.cancel();
		super.onPause();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		Log.v(LOG_TAG,"onDestroy");
		groupsGridLayout.removeAllViews();
		membersGridLayout.removeAllViews();
        bluetoothLinearLayout.removeAllViews();
		service.setActivity(null);
		unbindService(serviceConnection);
		///if (terminated)
	    //	service.terminate();
		super.onDestroy();
	}
	
	// an intermediary for sending a task to run on UI thread,
	private class MenuActivityTimerTask extends TimerTask {
		private static final String LOG_TAG = "MenuActivityTimerTask";
		@Override
		public void run() {
			//Log.v(LOG_TAG,"run");
			MenuActivity.this.runOnUiThread(new Tic());
		}
	}

	// a class representing the task the activity should perform on a tic.
	private class Tic extends TimerTask {
		private static final String LOG_TAG = "Tic";
		@Override
		public void run() {
			//Log.v(LOG_TAG,"run");
			if (service == null)
				return;
			if (!service.isWifiP2pEnabled()) {
				if (offlineMode.equals(OfflineMode.ONLINE)) {
					groupNameTextView.setText("wifi direct is disabled");
					service.setBringActivityToFront(true);
					popupsManager.enableWifiDialog.show();
				}
				else if (offlineMode.equals(OfflineMode.OFFLINE))
					groupNameTextView.setText("offline mode");
				else
					groupNameTextView.setText("wifi direct is disabled");
			}		
			else if (service.isConnectedToGroup().equals(ConnectionService.ConnectionState.PENDING))
				groupNameTextView.setText("connecting...");
			else if (service.isConnectedToGroup().equals(ConnectionService.ConnectionState.CONNECTED)) {
				groupNameTextView.setText("connected");//service.getCurrentGroup().getName());
				//service.refresh();
			}
			else if (service.isConnectedToGroup().equals(ConnectionService.ConnectionState.PREEMIE))
				groupNameTextView.setText("no available devices");
			else {
				groupNameTextView.setText("not connected");
                MY_IP.setText("WIFI IP:未連接");
				//service.refresh();
			}
			
			groupsGridLayout.removeAllViews();
			LayoutInflater li = (LayoutInflater) MenuActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            Collection<Group> availableGroups = service.getAvailableDevices();
			synchronized(availableGroups) {
				for (Group group : availableGroups) {
					LinearLayout layout = (LinearLayout) li.inflate(R.layout.group, null,false);
                    ((TextView) layout.findViewById(R.id.group_text)).setText(group.getName());
                    viewToGroupMap.put(layout, group);
                    groupsGridLayout.addView(layout);
				}
			}
			membersGridLayout.removeAllViews();

			Collection<Member> members = service.getGroupMembers();
			for (Member member : members) {
				LinearLayout layout = (LinearLayout) li.inflate(R.layout.member, null,false);
				((TextView) layout.findViewById(R.id.member_text)).setText(member.getName());
				viewToMemberMap.put(layout, member);
				membersGridLayout.addView(layout);
			}

		}
	}
}