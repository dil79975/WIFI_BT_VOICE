package adhoc.voip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
/**
 * Created by DIL on 2015/4/27.
 */
public class BluetoothManager {

    /** The is recording. */
    private boolean isRecording ;

    /** The send buffer size. */
    private int sendBufferSize;

    /** The receive buffer size. */
    private int receiveBufferSize;

    /** The Constant SAMPLE_RATE. */
    private static final int SAMPLE_RATE = 11025;

    /** The Constant SAMPLE_INTERVAL. */
    private static final int SAMPLE_INTERVAL = 20; // milliseconds



    private static BluetoothAdapter mBluetoothAdapter = null; // 用來搜尋、管理藍芽裝置

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 一定要是這組
    private final int REQUEST_ENABLE_BT=1;

    private String TAG = "bluetoothManager";

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    public ConnectedThread mConnectedThread;
    private int mState;
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private Set<BluetoothDevice> setPairedDevices;
    private List<BluetoothDevice> ssDevice = new ArrayList<BluetoothDevice>();

    private AudioTrack track;
    private AudioRecord recorder;
    private byte audioData[];



    public BluetoothManager() {
        Log.v(TAG,"constructor");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.start();


        sendBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT);
        receiveBufferSize = AudioTrack.getMinBufferSize( SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT );

        receiveBufferSize = Math.max(sendBufferSize, receiveBufferSize);
        sendBufferSize = Math.max(sendBufferSize, receiveBufferSize);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT,sendBufferSize);
        track = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, receiveBufferSize,
                AudioTrack.MODE_STREAM);
        isRecording = false;
    }

    public BluetoothManager(int i) {

    }


    public void send2(byte[] data){
        Log.v("GGGGG","data = " + data);
       // mConnectedThread.start();
        mConnectedThread.write(data);
    }


    public void send(){
        /*
        String a = "AAAAA";
        byte b[] = a.getBytes();
        mConnectedThread.write(b);
*/
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT,sendBufferSize);
        recorder.startRecording();


        if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
            Log.v(TAG,"failed to record");
        else
        {

            isRecording = true;
            //audioman.setSpeakerphoneOn(false);
            Log.v(TAG,"started recording");
            // audioData = new byte[sendBufferSize];

            new Thread (new Runnable()
            {
                public void run()
                {
                    audioData = new byte[sendBufferSize];
                    track.flush();
                    track.stop();
                    track.release();
                    track = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                            SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, receiveBufferSize,
                            AudioTrack.MODE_STREAM);
                    track.play();
                    while(isRecording){
                        recorder.read(audioData,0, sendBufferSize);
                    // sending the broadcast packet:
                        if(mConnectedThread!=null) {
                            Log.v("GGGGG", "audioData = " + audioData);
                            //mConnectedThread.write(audioData);
                            send2(audioData);
                        }else {
                            Log.v("GGGGG", "mConnectedThread = " + mConnectedThread);
                        }


                        try {
                            Thread.sleep(SAMPLE_INTERVAL, 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

                }
            }).start();

        }


    }

    public List<BluetoothDevice> showBondedDevice() {
        setPairedDevices = mBluetoothAdapter.getBondedDevices();

        if (setPairedDevices.size() > 0) {
            int i = 1;
            for (BluetoothDevice device : setPairedDevices) {
                ssDevice.add(device);
            }
        }
        return ssDevice;
    }

    public void connectToBluetooth(BluetoothDevice device){
        connect(device);
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");
        isRecording = false;

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

    public void cancle(){
        Log.d(TAG, "cancle");
        isRecording = false;
        Log.d(TAG, "recorder.getRecordingState() = " + recorder.getRecordingState());
        if(recorder.getRecordingState() == recorder.RECORDSTATE_RECORDING)
        recorder.stop();

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
            Log.v("GGGGG", "AcceptedThread啟動");
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
                synchronized (BluetoothManager.this) {
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
                Log.d(TAG, "Connect Fail = " + connectException);
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG,
                            "unable to close() socket during connection failure",closeException);}
                return;
            }
            // Do work to manage the connection (in a separate thread)
            synchronized (BluetoothManager.this) {
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
            byte[] buffer = new byte[receiveBufferSize];  // buffer store for the stream
            track.play();
            Log.v(TAG,"track : " + track);
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream

                        mmInStream.read(buffer);
                        Log.v("GGGGG", "buffer[]=" + buffer);

                        track.write(buffer, 0, receiveBufferSize);


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
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
