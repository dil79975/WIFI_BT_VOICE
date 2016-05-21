/**
 *
 */
package adhoc.voip;

import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Class ConnectionManager.
 *
 * @author Raz and Elad
 *
 * The connection manager is responsible for passing, receiving and handling
 * of the control messages over the network.
 * In order to do so, it uses the TCP protocol and passes connection instructions.
 * The connection instructions have the following unique structure:
 *
 * Message              |     args       |     sender      |    receiver     |     syntax
 * ---------------------+----------------+-----------------+-----------------+------------------
 * New member           | name           | any new member  | group owner     | N|name
 * ---------------------+----------------+-----------------+-----------------+------------------
 * members Update       | members names  | group owner     | all members     | U|name1|IP1|name2|IP2|...
 * ---------------------+----------------+-----------------+-----------------+------------------
 * R you there?         |      ---       | group owner     | all members     | R
 * ---------------------+----------------+-----------------+-----------------+------------------
 * I am here            |      ---       | any new member  | group owner     | I
 * ---------------------+----------------+-----------------+-----------------+------------------
 * Give me your profile |      ---       | any member      | any member      | G
 * ---------------------+----------------+-----------------+-----------------+------------------
 * my Profile           | profile        | any member      | any member      | P|name|email|...
 * ---------------------+----------------+-----------------+-----------------+------------------
 * Sms                  | message        | any member      | any member      | S|sender|message
 * ---------------------+----------------+-----------------+-----------------+------------------
 * Video request		| 		---		 | any member	   | any member	     |  V
 * ---------------------+----------------+-----------------+-----------------+------------------
 * approve video req	| 		---		 | any member	   | any member	     |  A
 * ---------------------+----------------+-----------------+-----------------+------------------
 * decline video req	| 		---		 | any member	   | any member	     |  D
 * ---------------------+----------------+-----------------+-----------------+------------------
 * close video activity | 			     | 	 any member	   | any member	     |  C
 */
public class ConnectionManager {
	private static final String LOG_TAG = "ConnectionManager";

	private final int CONTROL_PORT = 6666;
	private final int DATA_PORT = 6667;
	private final int UNIT_SEPARATOR = 87;
	private final int END_OF_TRANSMISSION = 56;

	private boolean active;
	private ExecutorService pool;

	private boolean isGroupOwner;
	private ServerSocket serverSocket;
	private Vector<Member> groupMembers;
	private Socket memberSocket;

	private ServerSocket dataServerSocket;

	private InetAddress ownerIPAddr;
	private Object monitor;



	public ConnectionManager() {
		Log.v(LOG_TAG,"constructor");
		Log.v("test-log", "constructor");
		groupMembers = new Vector<Member>();
		monitor = new Object();
		this.active = false;
	}

	/**
	 * Initiates the connection when the network is established.
	 *
	 * @param info the network info
	 */
	public void initiate(WifiP2pInfo info) {
		Log.v(LOG_TAG,"initiate: owner-" + info.isGroupOwner + ", addr-" + info.groupOwnerAddress);
		this.isGroupOwner = info.isGroupOwner;
		this.ownerIPAddr = info.groupOwnerAddress;
		pool = Executors.newCachedThreadPool();
		if (isGroupOwner) {
			pool.execute(new ServerSocketHandler());
			Log.v(LOG_TAG, "Owner execute ServerSoketHandler()");
		}
		else {
			pool.execute(new MemberSocketHandler());
			Log.v(LOG_TAG, "Member execute MemberSocketHandler()");
		}
		pool.execute(new DataHandler());

		this.active = true;
	}

	/**
	 * Terminates the connection.
	 * Closing connection sockets, and terminating threads.
	 */
	public void terminate() {
		Log.v(LOG_TAG,"terminate");
		if (active) {
			try {
				if (isGroupOwner) {
					serverSocket.close();

					for (Member member : groupMembers)
						member.getSocket().close();
				}
				else {
					memberSocket.close();
				}
                groupMembers.clear();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pool.shutdownNow();
			active = false;
		}
	}

	/**
	 * Gets the group members.
	 *
	 * @return the group members
	 */
	public Collection<Member> getGroupMembers() {
		//Log.v(LOG_TAG,"getGroupMembersNames");
		Vector<Member> members = new Vector<Member>();
		if (isGroupOwner)
		members.add(new Member(Build.MODEL,ownerIPAddr));
		for (Member member : groupMembers)
			members.add(member);
		return members;
	}

	private void sendNewMemberMessage(Socket socket) throws IOException {
		Log.v(LOG_TAG,"sendNewMemberMessage");
		OutputStream out = socket.getOutputStream();
		out.write((int)'N');
		out.write(UNIT_SEPARATOR); // unit separator
		byte[] name = Build.MODEL.getBytes();
		out.write(name);
		out.write(END_OF_TRANSMISSION); // end of transmission
		Log.v("test-log","send new member: " + name.toString()); // TODO - remove
	}


	// receives a new message (group owner only)
	private void receiveNewMemberMessage(Member member, Socket socket) throws IOException {
		Log.v(LOG_TAG, "receiveNewMemberMessage");
		InputStream in = socket.getInputStream();
		int b; // unit separator (31)
		b = in.read();
		String name = "";
		while (b != END_OF_TRANSMISSION) {
			name += (char)b;
			b = in.read();
		}
		Log.v("test-log", "received new member: " + name); // TODO - remove
		member.setName(name);
		member.setSocket(socket);
		synchronized(monitor) {
			groupMembers.add(member);
		}
	}

	// the group owner sends an update to all members. (names and IPs of all members)
	private void sendUpdateMessage(Socket socket) throws IOException {
		Log.v(LOG_TAG,"sendUpdateMessage");
		OutputStream out = socket.getOutputStream();
		out.write((int) 'U');
		for (Member member : getGroupMembers()) {
			out.write(UNIT_SEPARATOR); // unit separator
			byte[] nameBytes = member.getName().getBytes();
            out.write(nameBytes);
            out.write(UNIT_SEPARATOR); // unit separator
			byte[] IPBytes = member.getIP().getAddress();
			out.write(IPBytes);

        }
		out.write(END_OF_TRANSMISSION); // end of transmission
		Log.v("test-log", "send update"); // TODO - remove
	}

	// a member receives an update from owner
	private void receiveUpdateMessage(Socket socket) throws IOException {
		Log.v(LOG_TAG,"receiveUpdateMessage");
		InputStream in = socket.getInputStream();
		groupMembers.clear();
		int b = in.read();
		while (b != END_OF_TRANSMISSION) {
            Log.v(LOG_TAG,"收到資料b="+b);
			String name = "";
			byte[] IPBytes = new byte[4];
			b = in.read();
			while ((b != UNIT_SEPARATOR) && (b != END_OF_TRANSMISSION)) {
				name += (char)b;
				b = in.read();
			}

			IPBytes[0] = (byte) in.read();
			IPBytes[1] = (byte) in.read();
			IPBytes[2] = (byte) in.read();
			IPBytes[3] = (byte) in.read();
            Log.v(LOG_TAG,"IPBytes[0]="+IPBytes[0]);
            Log.v(LOG_TAG,"IPBytes[1]="+IPBytes[1]);
            Log.v(LOG_TAG,"IPBytes[2]="+IPBytes[2]);
            Log.v(LOG_TAG,"IPBytes[3]="+IPBytes[3]);
			b = in.read();
			groupMembers.add(new Member(name, InetAddress.getByAddress(IPBytes)));
			Log.v("test-log","received update: " + name + ":" + IPBytes[0] +"."+ IPBytes[1] +"."+ IPBytes[2] +"."+ IPBytes[3]); // TODO - remove
		}
	}

	// the group owner verifies that the member is still connected.
	private void sendRUThere(Socket socket) throws IOException {
		Log.v(LOG_TAG,"sendRUThere");
		OutputStream out = socket.getOutputStream();
		out.write((int) 'R');
		out.write(END_OF_TRANSMISSION); // end of transmission
		Log.v("test-log","send R U there"); // TODO - remove
	}

	// the member replies that it still connected.
	private void sendIMHere(Socket socket) throws IOException {
		Log.v(LOG_TAG, "sendIMHere");
		OutputStream out = socket.getOutputStream();

		out.write((int)'I');
		out.write(END_OF_TRANSMISSION); // end of transmission
		Log.v("test-log","send I M here"); // TODO - remove
	}



    // the member replies that it still connected.
    public void CLIENT_TO_GO() throws IOException {
        Log.v(LOG_TAG, "CLIENT_TO_GO");
        OutputStream out = memberSocket.getOutputStream();
        DataOutputStream out2 = new DataOutputStream( memberSocket.getOutputStream() );
        out.write(1);
        out.write(2);
        out.write(3);
        out.write('1');
        out.write('2');
        out.write('3');
        out.write(END_OF_TRANSMISSION); // end of transmission
        Log.v("test-log","亂傳"); // TODO - remove
    }

    public void GO_TO_CLIENT() throws IOException {
        Log.v(LOG_TAG, "GO_TO_CLIENT");
        /*
        OutputStream out = ss.getOutputStream();
        out.write(1);
        out.write(2);
        out.write(3);
        out.write('1');
        out.write('2');
        out.write('3');
        out.write(END_OF_TRANSMISSION); // end of transmission
        */
        Log.v("test-log","亂傳"); // TODO - remove
    }


	// server (group owner) runnable
	private class ServerSocketHandler implements Runnable {
		@Override
		public void run() {
			try {
				pool.execute(new Runnable() {
					@Override
					public void run() {
						try {
							while(true) {
								Thread.sleep(4000);
								Log.v(LOG_TAG, "ServerSocketHandler processing");
								synchronized(monitor) {
									for (Member member : groupMembers) {
										if (member.isActive()) {
                                            sendUpdateMessage(member.getSocket());
                                        }
										else
											member.decDeathCountdown();
										if (member.isAlive()) {
											member.setActive(false);
											sendRUThere(member.getSocket());
										}
										else
											groupMembers.remove(member);
									}
								}
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
				serverSocket = new ServerSocket(CONTROL_PORT);
				while (true) {
					Socket socket = serverSocket.accept();

					pool.execute(new GroupOwnerSocketHandler(socket));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// server (group owner) per member handler
	private class GroupOwnerSocketHandler implements Runnable {

		private Socket socket;
		private Member member;

		public GroupOwnerSocketHandler(Socket socket) {
			this.socket = socket;
			member = new Member();
		}
		@Override
		public void run() {
			Thread.currentThread().setName("GroupOwnerSocketHandler:"+this.socket);
			Log.v(LOG_TAG, "GroupOwnerSocketHandler processing:"+this.socket);
			try {
				InputStream in = socket.getInputStream();
				while (true) {
					int b = in.read();

                    if(b!=-1) Log.v(LOG_TAG, "b="+b);

					switch (b) {
					case (int)'N':
					{
						receiveNewMemberMessage(member, socket);
						break;
					}
					case (int)'I':
					{
						in.read(); // end of transmission (4)
						member.resetDeathCountdown();
						member.setActive(true);
						Log.v("test-log","recevied I M here");
					}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// member socket handler
	private class MemberSocketHandler implements Runnable {
		@Override
		public void run() {
			try {
				Log.v(LOG_TAG, "MemberSocketHandler processing");
				Thread.currentThread().setName("MemberSocketHandler");
				memberSocket = new Socket(ownerIPAddr,CONTROL_PORT);
				InputStream in = memberSocket.getInputStream();

                sendNewMemberMessage(memberSocket);

				while (true) {
					int b = in.read();
                    Log.v(LOG_TAG, "b="+b);
					switch (b) {
					case (int)'U':
					{
						receiveUpdateMessage(memberSocket);
						break;
					}
					case (int)'R':
					{
						in.read(); // end of transmission (4)
						sendIMHere(memberSocket);
						break;
					}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// data handler (for both owner and members)
	private class DataHandler implements Runnable {
		@Override
		public void run() {
			try {
				Log.v(LOG_TAG, "DataHandler processing");
				Thread.currentThread().setName("DataHandler");
				if(dataServerSocket==null)
					dataServerSocket = new ServerSocket(DATA_PORT);
				while (true) {
					Socket socket = dataServerSocket.accept();
					InputStream in = socket.getInputStream();
					OutputStream out = socket.getOutputStream();
					int b = in.read();
					if (b == 'G') { // give me your profile
						in.read();
						out.write('P');
						out.write(UNIT_SEPARATOR); // unit separator
						out.write(Build.MODEL.getBytes());
						out.write(END_OF_TRANSMISSION); // end
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
