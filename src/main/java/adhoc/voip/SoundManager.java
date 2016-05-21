/**
 * 
 */
package adhoc.voip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// TODO: Auto-generated Javadoc
/**
 * The Class SoundManager.
 * Manages sound transfer over UDP channel.
 * 
 * @author Raz and Elad
 */
public class SoundManager {

    private int i=0,j=0;

	/** The activity. */
	private Context context;

	
	/** The recorder. */
	private AudioRecord recorder;

	
	/** The audio data. */
	private byte audioData[];
	
	/** The destination address. */
	private InetAddress dstAddr;
	
	private DatagramSocket sendSocket;
	private DatagramSocket receiveSocket;
	private DatagramSocket ipSocket;
    private DatagramSocket logSocket;

	private boolean gotIP;
	
	private String myIP;
	
	private static final Random r = new Random();
    private static final Random r_LOGID = new Random();
    private String LOGID;
    private String BTDeviceName = null;
	
	private byte[] password;
	
	private static final int passwordLength = 10;
	

	/** The is recording. */
	private boolean isRecording , isReceiving;
	
	/** The send buffer size. */
	private int sendBufferSize;
	
	/** The receive buffer size. */
	private int receiveBufferSize;
	
	
	/** The Constant AUDIO_PORT. */
	private static final int AUDIO_PORT = 6665;
	
	/** The Constant GET_IP_PORT. */
	private static final int GET_IP_PORT = 1665;

    /** The Constant GET_IP_PORT. */
    private static final int LOG_PORT = 5566;

	/** The Constant AUDIO_MODE. */
	private static final int AUDIO_MODE = 0 ;
	
	/** The Constant GET_IP_MODE. */
	private static final int GET_IP_MODE = 1 ;

    /** The Constant LOG_START_MODE. */
    private static final int LOG_START_MODE = 2 ;

    /** The Constant LOG_END_MODE. */
    private static final int LOG_END_MODE = 3 ;


    /** The Constant SAMPLE_RATE. */
	private static final int SAMPLE_RATE = 8000;
    
    /** The Constant SAMPLE_INTERVAL. */
	private static final int SAMPLE_INTERVAL = 20; // milliseconds
    
    /** The Constant LOG_TAG. */
	private static final String LOG_TAG = "soundManager";

    /** The track. */
    private AudioTrack track;


    /** Bluetooth **/
    private static BluetoothAdapter mBluetoothAdapter = null; // 用來搜尋、管理藍芽裝置


    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private String TAG = "bluetoothManager";

    private Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    public ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private Set<BluetoothDevice> setPairedDevices;
    private List<BluetoothDevice> ssDevice = new ArrayList<BluetoothDevice>();
    private boolean isBluetoothDevice = false;
    private PopupsManager popupsManager;
    private LogManager logManager = new LogManager();
    private TimeManager timeManager = new TimeManager();


    /**
     * Instantiates a new sound manager.
     *
     * @param context the application context
     */

    public SoundManager(Context context) {
    	this.context = context;
    	Log.v(LOG_TAG, "entered the constructor");
        audioData = null;
        try {
			sendSocket = new DatagramSocket();
			dstAddr = InetAddress.getByName("192.168.49.255");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        popupsManager = new PopupsManager(this.context);


		sendBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT);
		receiveBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		receiveBufferSize = Math.max(sendBufferSize, receiveBufferSize);
		sendBufferSize = Math.max(sendBufferSize, receiveBufferSize);

/*
        receiveBufferSize = 10240;
        sendBufferSize = 10240;
*/

		gotIP = false;
		myIP = new String();

		isRecording = false;
        isReceiving = false;

		password = new byte[passwordLength];

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        this.log();
        this.start(); /** start AcceptThread **/

   }

    public void setmHandler(Handler handler){
        mHandler = handler;
        Log.v("SM層", "mHandler="+mHandler);
    }

    /**
     * Receive audio packages.
     */
    public void receive(){
        final int[] c = {0};
        Thread thread = new Thread(new Runnable() {
                        @Override
            public void run() {
				try {
					receiveSocket = new DatagramSocket(AUDIO_PORT);
					byte[] buffer = new byte[receiveBufferSize];
					DatagramPacket pack = new DatagramPacket(buffer, receiveBufferSize);;
                    while(true){
                    	receiveSocket.receive(pack);
                       //	Log.v(LOG_TAG,"recieved audio packet");
                    	if(!pack.getAddress().toString().equals(myIP)) //so we won't hear what we have recorded
						{
                            trackWrite(pack.getData(), 0, pack.getLength(),"WIFI");

                            if(isBluetoothDevice) {
                                sendToBluetoothDevice(pack.getData());

                                Log.v("GGGGG", c[0]++
                                        +"Relaying data["
                                        + pack.getData()
                                        + "] to device : []");
                            }
						}
                    }
				}
                catch (IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
                    e.printStackTrace();
                }
                        } // end run
        });
        thread.setName("receive");
        thread.start();
    }

    public void log(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String START_FLAG = "START";
                    String END_FLAG = "END";
                    logSocket = new DatagramSocket(LOG_PORT);
                    byte[] buffer = new byte[100];
                    DatagramPacket pack = new DatagramPacket(buffer, 100);
                    Log.v("GG", "log thread run");
                    while(true) {
                        logSocket.receive(pack);
                        String[] token = new String(pack.getData(),0,pack.getLength()).split("@");

                        if (token[0].equals(START_FLAG) && !pack.getAddress().toString().equals(myIP)){
                            logManager.Start(token[1],token[2]);
                            Log.v("GG", "收到來自[" + token[1] + "]的訊息:" + token[0] +" "+ token[2]);}

                        else if (token[0].equals(END_FLAG) && !pack.getAddress().toString().equals(myIP)){
                            logManager.End(token[1],token[2]);
                            Log.v("GG", "收到來自[" + token[1] + "]的訊息:" + token[0] +" "+ token[2]);}
                    }
                }
                catch (IOException e){
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } // end run
        });
        thread.setName("log");
        thread.start();
    }


    public void trackWrite(byte[] a,int b,int c,String d) throws InterruptedException {
        if(d=="WIFI" && track!=null) {
            track.write(a, b, c);
        }

        if(d=="BT" && track!=null) {
            track.write(a, b, c);
        }
    }

    /**
     * Send audio packages.
     */
    public void sendAudio()
    {
        isReceiving = true;
        audioData = new byte[sendBufferSize];

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, sendBufferSize);

        recorder.startRecording();

        if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
            Log.v(LOG_TAG,"failed to record");

             track = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                     SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                     AudioFormat.ENCODING_PCM_16BIT, receiveBufferSize,
                     AudioTrack.MODE_STREAM);

             new Thread(new Runnable() {
                 @Override
                 public void run() {
                     send(AUDIO_MODE);
                 }
             }).start();
    }

	/**
	 * Send a single packet.
	 * Also responsible for discovering the IP
	 *
	 * @param mode the operation mode - getting IP or regular send.
	 */
	public void send(int mode)
	{
		try
		{
			if(mode == GET_IP_MODE) // so we want to sent dummy data to our selfs
			{
                int readBytes = 10;
				//byte [] dummyBuffer = new byte[sendBufferSize];
				for(int i =0 ; i < 10 ; i++)
				{
					r.nextBytes(password);
				}
				DatagramPacket setIPPacket = new DatagramPacket(password, readBytes,dstAddr,GET_IP_PORT);
				sendSocket.send(setIPPacket);
			}

            else if(mode == LOG_START_MODE)
            {
                InetAddress Addr = InetAddress.getByName("192.168.49.255");
                String LOG_FLAG = "START";

                LOGID=Build.MODEL;
                LOGID = LOGID +"_"
                        + String.valueOf(r_LOGID.nextInt(10000))
                       ;

                LOG_FLAG = LOG_FLAG + "@" + LOGID + "@" + timeManager.getTime()+"@";
                while (LOG_FLAG.length()!=1380)
                    LOG_FLAG = LOG_FLAG + "0";

                DatagramPacket setLOGPacket =
                        new DatagramPacket(LOG_FLAG.getBytes(),LOG_FLAG.getBytes().length,Addr,LOG_PORT);
                sendSocket.send(setLOGPacket);

                if(isBluetoothDevice) {
                    Log.v("GGG", "LOG_FLAG.getBytes()=" + LOG_FLAG.getBytes().length);
                    sendToBluetoothDevice((LOG_FLAG).getBytes());
                }

            }

            else if(mode == LOG_END_MODE)
            {
                InetAddress Addr = InetAddress.getByName("192.168.49.255");
                String LOG_FLAG = "END";

                LOG_FLAG = LOG_FLAG + "@" + LOGID + "@" + timeManager.getTime()+"@";
                while (LOG_FLAG.length()!=1380)
                    LOG_FLAG = LOG_FLAG + "0";


                DatagramPacket setLOGPacket =
                        new DatagramPacket(LOG_FLAG.getBytes(),LOG_FLAG.getBytes().length,Addr,LOG_PORT);
                sendSocket.send(setLOGPacket);

                if(isBluetoothDevice) {
                    Log.v("GGG", "LOG_FLAG.getBytes()" + LOG_FLAG.getBytes());
                    sendToBluetoothDevice(LOG_FLAG.getBytes());
                }

            }

			else //sending audio
			{
                int c1 = 0  , c2 = 0 ;
                Log.v(LOG_TAG,"started recording");

				track.flush();
				track.stop();
				track.play();

                while(isReceiving){

                    while(isRecording)
                    {
                        int Bytes = recorder.read(audioData,0, sendBufferSize);
                        sendToWifiDevice(audioData, Bytes);
                    //    Log.v("GGGGG", c1++ +"Sending data[" + audioData + "] Bytes :" + Bytes + "   to device : [" + dstAddr + "]");

                        if(isBluetoothDevice) {

                            String LOG_FLAG = "SENDING" + "@" + Build.MODEL + "@" + timeManager.getTime() + "@";
                            while (LOG_FLAG.length()!=100)
                                LOG_FLAG = LOG_FLAG + "0";
                            byte sendbyte[] = byteMerger(LOG_FLAG.getBytes(),audioData);

                            sendToBluetoothDevice(sendbyte);
                        //    Log.v("GGGGG", c2++ + "Sending data[" + audioData + "] Bytes :" + Bytes + "   to device : [" + BTDeviceName + "]");
                        }
                    }
                    // Thread.sleep(1000);
                }
            }
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    public void SAY(){
        isRecording = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                send(LOG_START_MODE);
            }
        }).start();
    }

    public void stopSAY(){
        isRecording = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                send(LOG_END_MODE);
            }
        }).start();
    }

	/**
	 * Initiate IP address.
	 * Done by sending a packet to myself and checking the sender IP
	 */
	public void initiateIP()
	{
		gotIP = false;
		new Thread (new Runnable(){
	    	 public void run()
	    	 {
	    		 try 
	    		 {

                    if(ipSocket==null)
                        ipSocket = new DatagramSocket(GET_IP_PORT);
					int timeout = 100;
					byte[] buffer = new byte[receiveBufferSize];
		    		DatagramPacket pack = new DatagramPacket(buffer, receiveBufferSize);
		    		while(!gotIP && timeout > 0)
					{
		    			timeout -- ;
		    			send(GET_IP_MODE);
						ipSocket.receive(pack);
                    	Log.v(LOG_TAG,"recieved ip packet");
                    	gotIP = true;
                    	//check the received data for passwork match
                    	for(int i = 0 ; i < SoundManager.passwordLength ; i++)
                    	{
                    		if(pack.getData()[i] != SoundManager.this.password[i])
                    			gotIP =false;
                    	}                       										
					}
					// received data match password, save IP
					SoundManager.this.myIP = pack.getAddress().toString();
					Log.i(LOG_TAG,"myIP : " + SoundManager.this.myIP);

                     Message msg = mHandler.obtainMessage(Constants.MESSAGE_MYIP);
                     Bundle bundle = new Bundle();
                     bundle.putString("myIP",myIP);
                     msg.setData(bundle);
                     mHandler.sendMessage(msg);

		    	 }
	    	 	 catch (IOException e)
	    		 {
					// TODO Auto-generated catch block
					e.printStackTrace();
				 }
	    		}}).start();
	    		 
	}

	
	/**
	 * Stop recording.
	 */
	public void stopRecording()
	{
		isRecording = false; //don't record and send no more.
        isReceiving = false;
		recorder.stop();
		recorder.release();

		track.flush();
        track.stop();
        track.release();

	}  

	/**
	 * Shut down.
	 */
	public void shutDown() {
		if (recorder != null)
			recorder.release();
		sendSocket.disconnect();
		sendSocket.close();
		receiveSocket.disconnect();
		receiveSocket.close();
	    track.flush();
	    track.stop();
	    track.release();
	}


/** For bluetooth **/
    /** For bluetooth **/
        /** For bluetooth **/
            /** For bluetooth **/


    public void sendToBluetoothDevice(byte[] data){
//        if(mConnectedThread!=null)
//            mConnectedThread.write(data);
        write(data);
    }

    public void sendToWifiDevice(byte[] data,int readBytes){
        DatagramPacket p = new DatagramPacket(data, readBytes, dstAddr, 6665);
        try {
            sendSocket.send(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendToWifiDevice_LOG(byte[] data,int readBytes){
        DatagramPacket p = new DatagramPacket(data, readBytes, dstAddr, 5566);
        try {
            sendSocket.send(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<BluetoothDevice> showBondedDevice() {
        setPairedDevices = mBluetoothAdapter.getBondedDevices();

        if (setPairedDevices.size() > 0) {
            for (BluetoothDevice device : setPairedDevices) {
                ssDevice.add(device);
            }
        }
        return ssDevice;
    }

    public void connectToBluetooth(BluetoothDevice device){
        connect(device,true);
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
  //      mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity

        Log.v("GG2", "mHandler="+mHandler);

        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);

        isBluetoothDevice=true;
        BTDeviceName = device.getName();
    }

    /**
     * Stop all threads
     */
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

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        isBluetoothDevice=false;
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        SoundManager.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        isBluetoothDevice=false;
        // Send a failure message back to the Activity
        Log.v("GG", "mHandler="+mHandler);
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        SoundManager.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (SoundManager.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SoundManager.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] tempbuffer = new byte[100+receiveBufferSize];
            byte[] tempbuffer2 = new byte[100+receiveBufferSize];
            byte[] logbuffer = new byte[100];
            byte[] buffer = new byte[receiveBufferSize];
            int dataReadWriteCursor=0;

            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(tempbuffer);
                    Log.v("GGGGG", "Bytes=" + bytes);

                    // construct a string from the buffer
/*
                    for (int i = 0; i < bytes; i++) {
                        tempbuffer2[dataReadWriteCursor] = tempbuffer2[i];
                        dataReadWriteCursor++;
                    //    Log.v("GGGGG", "dataReadWriteCursor=" + dataReadWriteCursor);
                    }
*/
                    if(bytes!=0){
                        for (i = 0; i < 100; i++)
                            logbuffer[i] = tempbuffer[i];

                        for (j = 100; j < 1380; j++)
                            buffer[j - 100] = tempbuffer[j];

                        String[] token = new String(logbuffer).split("@");
                        for(i=0;i<token.length;i++) {
                            if(token[i]!=null) {
                                Log.v("GG", "token["+i+"]=" + token[i]);
                            }
                        }

                        trackWrite(buffer, 0, receiveBufferSize, "BT");

                        if(token[0].toString()=="START" || token[0].toString()=="END") {
                            Log.v("GG", "轉送控制訊息出去囉~:" + token[0]);
                            sendToWifiDevice_LOG(logbuffer, 100);
                        }

                        sendToWifiDevice(buffer,buffer.length);
                    }

                    if(mmInStream.available()<1380){
                        tempbuffer=null;
                        tempbuffer=new byte[1380];
                        Log.v("GG", "?!!@@" );
                    }


/*
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
*/

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    SoundManager.this.start();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


}
