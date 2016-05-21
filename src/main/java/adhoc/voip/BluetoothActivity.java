package adhoc.voip;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;


public class BluetoothActivity extends Activity {


    private static BluetoothAdapter mBluetoothAdapter = null; // 用來搜尋、管理藍芽裝置

    private final String TAG = "BluetoothActivity";

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 一定要是這組
    private final int REQUEST_ENABLE_BT=1;
    private Set<BluetoothDevice> setPairedDevices;
    private List<BluetoothDevice> queriedDevices = new ArrayList<BluetoothDevice>();
    private List<BluetoothDevice> ssDevice = new ArrayList<BluetoothDevice>();
    private List<String> pairedArrayAdapter =  new ArrayList<String>();
    private List<String> searchArrayAdapter = new ArrayList<String>();
    private ListView pairedList;
    private ListView searchList;
    private RadioGroup radioGroup;
    private RadioButton radioButton1, radioButton2, radioButton3;
    private ToggleButton ON_OFF_BUTTON;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    public ConnectedThread mConnectedThread;
    private int mState;
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 註冊一個BroadcastReceiver，等等會用來接收搜尋到裝置的消息
        IntentFilter searchFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mSearchFilter, searchFilter);
        registerReceiver(mFoundFilter, foundFilter);

     //   pairedList = (ListView) findViewById(R.id.PairedList);
        searchList = (ListView) findViewById(R.id.SearchList);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioButton1 = (RadioButton) findViewById(R.id.radioButton1);
        radioButton2 = (RadioButton) findViewById(R.id.radioButton2);
        radioButton3 = (RadioButton) findViewById(R.id.radioButton3);
        ON_OFF_BUTTON = (ToggleButton) findViewById(R.id.flowEnableToggleButton);

        radioGroup.setOnCheckedChangeListener(mChangeRadio);

        ON_OFF_BUTTON.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Toast.makeText(getApplicationContext(),"尚未指定藍芽裝置",Toast.LENGTH_SHORT).show();
            }
        });

        //start();

    }
    protected void onResume(){
        super.onResume();
    }

    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mSearchFilter);
        unregisterReceiver(mFoundFilter);

    }

    public void Bluetooth_appear(View v){
        Intent discoverableIntent = new
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
        startActivity(discoverableIntent);
    }

    public void sendString(View v){
        String a = "AAAAA";
        byte b[] = a.getBytes();
        mConnectedThread.write(b);
    }

    public void searchBluetooth(View v){

        if (mBluetoothAdapter == null) {
            // 如果裝置不支援藍芽
            Toast.makeText(this, "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        // 如果藍芽沒有開啟
        if (!mBluetoothAdapter.isEnabled()) {
            // 發出一個intent去開啟藍芽，
            Intent mIntentOpenBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(mIntentOpenBT, REQUEST_ENABLE_BT);
        }
        else {
            // 取得目前已經配對過的裝置
            setPairedDevices = mBluetoothAdapter.getBondedDevices();

            // 如果已經有配對過的裝置
            if (setPairedDevices.size() > 0) {
                int i=1;
                // 把裝置名稱以及MAC Address印出來
                for (BluetoothDevice device : setPairedDevices) {
                    ssDevice.add(device);

                    pairedArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    if(i==1) {
                        radioButton1.setText(device.getName() + "\n" + device.getAddress());
                    }
                    if(i==2) {
                        radioButton2.setText(device.getName() + "\n" + device.getAddress());
                    }
                    if(i==3) {
                        radioButton3.setText(device.getName() + "\n" + device.getAddress());
                    }
                    i++;
                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.v("searchDevices","Discovery");
                    mBluetoothAdapter.startDiscovery();
                }
            }).start();
        }

    }

    private RadioGroup.OnCheckedChangeListener mChangeRadio = new
            RadioGroup.OnCheckedChangeListener(){

                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    // TODO Auto-generated method stub
                    if(i==radioButton1.getId()){
                        if(ssDevice.get(0)!=null) {
                            connect(ssDevice.get(0));
                            Toast.makeText(getApplicationContext(),"", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else if(i==radioButton2.getId()) {
                        if (ssDevice.get(1) != null) {
                            connect(ssDevice.get(1));
                            Toast.makeText(getApplicationContext(),"", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else if(i==radioButton3.getId()){
                        if(ssDevice.get(2)!=null) {
                            connect(ssDevice.get(2));
                            Toast.makeText(getApplicationContext(),"", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };




    private final BroadcastReceiver mSearchFilter = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.v("searchDevices","Search Success");
        }
    };

    private final BroadcastReceiver mFoundFilter = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.v("searchDevices","Search Complete Device");
            // 當收尋到裝置時
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                // 取得藍芽裝置這個物件
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                queriedDevices.add(device);

                searchArrayAdapter.add(device.getName() + "\n" + device.getAddress());

                final ArrayAdapter<String> listAdapter =
                        new ArrayAdapter<String>(BluetoothActivity.this,android.R.layout.simple_list_item_1,searchArrayAdapter);
                searchList.setAdapter(listAdapter);

                listAdapter.notifyDataSetChanged();

                searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        connect(queriedDevices.get(i));

                    }
                });

            }
        }
    };

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }



    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread == null) {

            } else {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);

        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    // ConnectedThread
    public synchronized void connected(BluetoothSocket socket,BluetoothDevice device) {
        Log.d(TAG, "開始connected");

        // Cancel the thread that completed the connection
        if (mConnectThread == null) {

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        setState(STATE_CONNECTED);
    }

    //  thread stop
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] out) { // Create temporary object
        ConnectedThread r; // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        } // Perform the write unsynchronized r.write(out); }
    }

    private void connectionFailed() {
        setState(STATE_LISTEN);
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
    }



    public class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BLUE", MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            Log.v("GGGGG","AcceptedThread啟動");
            // Keep listening until exception occurs or a socket is returned
            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                    Log.v("GGGGG","接受Socket" + socket);
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    new ConnectedThread(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e("AcceptedThread", "accept() failed", e);
                    }
                    break;
                }
            }
            // If a connection was accepted
            if (socket != null) {
                synchronized (BluetoothActivity.this) {
                    switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e("AcceptedThread", "Could not close unwanted socket", e);
                            }
                            break;
                    }
                }
            }


        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {

            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }

            mmSocket = tmp;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.v("GGGGG","已連結" + mmSocket);
            } catch (IOException connectException) {
                connectionFailed();
                Log.d(TAG, "Connect Fail" + connectException);
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG,
                            "unable to close() socket during connection failure",closeException);}
                return;
            }
            // Do work to manage the connection (in a separate thread)
            synchronized (BluetoothActivity.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }



        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Log.v("GGGGG","收到資料" + bytes);


                    // Send the obtained bytes to the UI activity
              //      mHandler.obtainMessage("MESSAGE_READ", bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Log.v("GGGGG","ConnectedThread.write 運作");
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
