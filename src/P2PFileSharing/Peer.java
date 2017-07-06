package P2PFileSharing;


import java.io.*;
import java.net.*;
import java.util.*;



import FileProcess.FileProcess;

import java.sql.Timestamp;
import java.nio.*;



public class Peer extends Thread{
	int Port;
	static int MyID;
	int PieceNum;
	String filePath;
	
	//int[] piece;
	//BitSet piece;
	static boolean[] interest;
	boolean[] chokeOther;
	boolean[] chokeMe;
	boolean unchokeOrOpUnchoke; // true is that unchoke is large, false is that opunchoke is large

	String[][] IDIPport;
	//int[][] PeerPiece;
	static BitSet[] PeerPiece;	
	static int PeerNum;
	int socketNum = 0;
	int unchokeInterval;
	int optimisticUnchokingInterval;
	int numberOfPreferredNeighbors;
	int frontInterval;
	int rearInterval;
	static boolean allContinue;
	static boolean[] requestSent;
	static boolean[] peerComplete;
	static int unchokeNeiborNum;
	static int[] selectedNeibor;
	static int optimisticUnchoke;
	static int PieceSize;
	static String fileName;
	static int[] peerRate;
	static String ClientFilepath;
	static Hashtable<Integer, Integer> peerID_socketNum = new Hashtable<Integer, Integer>();
	static Socket[] sockets;
	static Thread[] Threads;
 	static ObjectOutputStream[] outputs;
	Hashtable<String, Integer> IPID = new Hashtable<String, Integer>();
	

	
	public Peer(int PeerID, int Port, String filePath, int PieceNum, String[][] IDIPport, Hashtable<String, Integer> IPID,
			int PeerNum, int unchokeInterval, int optimisticUnchokingInterval, int numberOfPreferredNeighbors, String fileName, String ClientFilepath, int PieceSize)
	{
		this.Port = Port;
		this.MyID = PeerID;
		this.filePath = filePath;
		this.PieceNum = PieceNum;
		this.IDIPport = IDIPport;
		this.IPID = IPID;
		this.interest = new boolean[PeerNum];
		this.chokeMe = new boolean[PeerNum];
		this.chokeOther = new boolean[PeerNum];
		PeerPiece = new BitSet[PeerNum];
		this.PeerNum = PeerNum;
		this.unchokeInterval = unchokeInterval;
		this.optimisticUnchokingInterval = optimisticUnchokingInterval;
		this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
		this.fileName = fileName;
		this.ClientFilepath = ClientFilepath;
		this.PieceSize = PieceSize;
		this.peerRate = new int[PeerNum];
		this.unchokeNeiborNum = numberOfPreferredNeighbors + 1;
		this.optimisticUnchoke = -1;
		this.requestSent = new boolean[PieceNum];
		this.peerComplete = new boolean[PeerNum];
		this.allContinue = true;
		for(int i = 0; i < PeerNum; i++){
			PeerPiece[i]=new BitSet(PieceNum);
			PeerPiece[i].clear(); 
			if(IDIPport[i][3].equals("1")) {
				PeerPiece[i].flip(0,PieceNum);
			}
		}
		for(int i = 0; i < PieceNum; i++){
			requestSent[i] = false;
		}
		for(int i = 0; i < PeerNum; i++){
			interest[i] = false;
			chokeMe[i] = true;
			chokeOther[i] = true;
			peerRate[i] = 0;
			peerComplete[i] = false;
		}
		peerComplete[0] = true;
	}
	
	static int comparePiece(int MyID, int peerID, int PieceNum){
		int requestPiece;
		List<Integer> wantPiece = new LinkedList<Integer>();
		for(int i = 0; i<PieceNum; i++){
			if (PeerPiece[peerID - 1001].get(i) && !PeerPiece[MyID - 1001].get(i)){
				wantPiece.add(i);
			}
		}
		
		if(wantPiece.size() == 0){
			return -1;
		}
		Random rand = new Random();

		requestPiece = wantPiece.get(rand.nextInt(wantPiece.size()));
		
		return requestPiece;
	}
	
    static boolean log(String msg) {
        //This function write 'msg' into the log file. return true if success, false otherwise.
        String name = "log_peer_" +  MyID + ".log";
        java.util.Date date = new java.util.Date();
        Timestamp ts = new Timestamp(date.getTime());
        msg = "[" + ts + "]: " + msg;
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(name, true)))) {
            out.println(msg);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
            System.out.println(e);
            return false;
        }
        return true;
    }
	
    //converse any byte array to bitset
	public static BitSet fromByteArray(byte[] bytes, int length, int PeerID){
	    BitSet bits = new BitSet();
	    if(bytes.length == 0){
	    	return bits;
	    }
	    for(int i = 0; i < length; i++){
	    	if( (bytes[i/8] & (1 << (i % 8))) > 0){
	    		bits.set(i);
	    		PeerPiece[PeerID - 1001].set(i);
	    	}
	    }
	    return bits;
	}
	
	//converse bitset to any byte array( 0 at end bytes will not be ignored)
	public static byte[] BitSettoByteArray(BitSet bitset, int PieceNum){
		byte[] byteArray = new byte[(PieceNum + 7) / 8];
	    for (int i=0; i<bitset.length(); i++) {
	        if (bitset.get(i)) {
	            byteArray[i/8] |= 1<<(i%8);
	        }
	    }
		return byteArray;
	}
    

	
	public static byte[] readfile(int index)
	{
		byte[] buf = new byte[PieceSize];
		try{
		
			File file = new File(ClientFilepath +  fileName + index + ".part");
		
			//System.out.println("Size of file" + (int) file.length());
		
			DataInputStream FileInput = new DataInputStream(new BufferedInputStream(new FileInputStream(ClientFilepath +  fileName + index + ".part")));
			
			//send file

            while (true) {
                int read = 0;
                if (FileInput != null) {
                    read = FileInput.read(buf);
                }

                if (read == -1) {
                    break;
                }
            }
            
            return buf;
            //Close socket
		} 
		catch (Exception e) {
        e.printStackTrace();
		}
		return buf;
        //Close socket
	}
	
	public static void sendhave(int PeerNum, int MyID, byte[] index)
	{
		Message haveMessage = new Message(4, (byte)4, index);
		for(int i = 0; i < PeerNum; i++){
			if(MyID == (i+1001));
			else{
				send(i+1001, haveMessage);
			}
		}
	}
	
	public static void sendComplete(int PeerNum, int MyID){
		Message completeMessage = new Message(0, (byte)9, null);
		for(int i = 0; i < PeerNum; i++){
			if(MyID == (i+1001));
			else{
				send(i+1001, completeMessage);
			}
		}
	}
	
	public synchronized static void send(int peerID, Object message){
		if(peerID_socketNum.containsKey(peerID)){
			int index = peerID_socketNum.get(peerID);
			try {
				outputs[index].writeObject(message);
				outputs[index].flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			//System.out.println("peer " + peerID + "has not been connected!" );
		}
	}
	
	public static int byteArrayToInt(byte[] b) 
	{
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}

	public static byte[] intToByteArray(int a)
	{
	    return new byte[] {
	        (byte) ((a >> 24) & 0xFF),
	        (byte) ((a >> 16) & 0xFF),   
	        (byte) ((a >> 8) & 0xFF),   
	        (byte) (a & 0xFF)
	    };
	}
	
	@SuppressWarnings("deprecation")
	public static void finish() throws IOException{
		boolean allComplete = true;
		for(int i = 0; i<PeerNum; i++ ){
			allComplete = (allComplete && peerComplete[i]);
			System.out.println("allComplete is " + allComplete + " and peer i is " + peerComplete[i]);
		}
		if(allComplete){
			try {
				sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.exit(0);
			/*
			for(int i = 0; i< PeerNum; i++){
				outputs[i].close();
				sockets[i].close();
				Threads[i].stop();
				
			}
			*/
			allContinue = false;
		}
	}
	
	public void run()
	{
		sockets = new Socket[PeerNum];
		Threads = new Thread[PeerNum];
	 	outputs = new ObjectOutputStream[PeerNum];
	 	
	 	Choke chokeThread = new Choke();
	 	chokeThread.start();
	 	
	 	try{
	 		int clientNum = MyID - 1001;
	 		for(int i = 0; i < clientNum; i++){
	 			//System.out.println("IP: " + IDIPport[i][1] + "; Port: " + IDIPport[i][2]);
	 			Socket sol = new Socket(IDIPport[i][1], Integer.parseInt (IDIPport[i][2]));
	 			//System.out.println("Successfully build client!");
	 			sol.getOutputStream().flush();
	 			sockets[socketNum] = sol;
	 			outputs[socketNum] = new ObjectOutputStream(sol.getOutputStream());
	 			Threads[socketNum] = new Client(sol, MyID , interest, PeerPiece, Integer.parseInt(IDIPport[i][0]));
	 			Threads[socketNum].start();
	 			peerID_socketNum.put(Integer.parseInt(IDIPport[i][0]), socketNum);
				   
	 			socketNum++;
	 		}
	 	}
		catch(IOException ioException)
		{
			System.out.println("Cannot run client");
		}
	 	
	 	
		try
		{
			ServerSocket listener = new ServerSocket(Port);
			while(allContinue)
			{

			   Socket sol = listener.accept();
			   String IP = sol.getInetAddress().toString();
			   
			   int ID = IPID.get(IP.substring(1));
			   System.out.println("ID: "+ ID);
			   sockets[socketNum] = sol;
			   outputs[socketNum] = new ObjectOutputStream(sol.getOutputStream());
			   Threads[socketNum] = new Server(sol, MyID , interest, PeerPiece, ID);
			   Threads[socketNum].start();
			   
			   peerID_socketNum.put(ID, socketNum);			   
			   socketNum++;
			}
			//listener.close();
		}
			   
		catch(IOException ioException)
		{
			System.out.println("Cannot run server");
		}
			
		
	}
	
	public class Client extends Thread{
		private Socket requestSocket;
	    private ObjectInputStream read; // stream read from socket		
		//private ObjectOutputStream write;    //stream write to the socket
		
		private int MyID;
		private int peerID;
		
		public int[] piece;
		public boolean[] interest;
		private BitSet[] PeerPiece;
		
		public Client(Socket requestSocket, int MyID,  boolean[] interest, BitSet[] PeerPiece, int peerID){
			this.requestSocket = requestSocket;
			this.MyID = MyID;
			this.peerID = peerID;
			this.interest = interest;
			this.PeerPiece = PeerPiece;
		}
		
		public void run(){
			//System.out.println("New client oooo!");
			HandShake shakeHand = new HandShake(MyID);
			send(peerID, shakeHand);
			log("Peer [" + MyID + "] makes a connection to Peer [" + peerID + "]");
			try {
				read = new ObjectInputStream(requestSocket.getInputStream());
				while(true){
					Object receivedMessage = read.readObject();
					if(receivedMessage instanceof HandShake){
						System.out.println("HandShake message from peer : "+peerID );
						log("Peer [" + MyID + "] is connected from Peer [" + peerID + "]");
						System.out.println("bitfield: " + PeerPiece[MyID - 1001].toString());
						byte[] bytepayload = PeerPiece[MyID-1001].toByteArray();
				
						Message bitFiledMessage = new Message(bytepayload.length, (byte)5, bytepayload);
						send(peerID,bitFiledMessage);
						log("Peer [" + MyID + "] sent 'bitfield' message to Peer [" + peerID + "]");
					}
					else if(receivedMessage instanceof Message){
						Message m = (Message) receivedMessage;
						if( m.type == (byte)5){
							System.out.println("bitfiled message received from " + peerID);
							log("Peer [" + MyID + "] received the 'bitfield' message from Peer [" + peerID + "]");
							fromByteArray(m.payload, PieceNum, peerID);
							System.out.println("peerPiece bitfield:  "+PeerPiece[peerID - 1001].toString());	
							int wantNum = comparePiece(MyID, peerID, PieceNum);
							Message interesting;
							if(wantNum!=-1){
								interesting = new Message(0, (byte)2, null);
								log("Peer [" + MyID + "] sent the 'interested' message to Peer [" + peerID + "]");
							}
							else{
								interesting = new Message(0, (byte)3, null);
								log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + peerID + "]");
							}
							send(peerID, interesting);							
						}
						else if(m.type == (byte)2){
							log("Peer [" + MyID + "] received 'interested' message from Peer [" + peerID + "]");
							interest[peerID - 1001] = true;
							//System.out.println("Received interest");
						}
						else if(m.type == (byte)3){
							log("Peer [" + MyID + "] received 'not interested' message from Peer [" + peerID + "]");
							interest[peerID - 1001] = false;
							//System.out.println("Received not interest");
						}
						else if(m.type == (byte)1){
							log("Peer [" + MyID + "] is unchoked by Peer [" + peerID + "]");
							chokeMe[peerID - 1001] = false;
							int index = comparePiece(MyID, peerID, PieceNum);
							if(index != -1 && !requestSent[index]){
								System.out.println("send request for piece index: " + index);
								byte[] indexBytes = intToByteArray(index);
								Message requestMessage = new Message(4, (byte)6, indexBytes);
								send(peerID, requestMessage);
								requestSent[index] = true;
								log("Peer [" + MyID + "] send request message to Peer [" + peerID + "] for piece [" + index +"]" );
							}
							else{
								//System.out.println("Unchoke from not interested peer" + peerID);
							}
						}
						else if(m.type == (byte)0){
							log("Peer [" + MyID + "] is choked by Peer [" + peerID + "]");
							chokeMe[peerID - 1001] = true;
						}
						else if(m.type == (byte)6){
							log("Peer [" + MyID + "] received request from Peer [" + peerID + "]");
							int index = byteArrayToInt(m.payload);
							//System.out.println("request from Peer "+ peerID +" of piece index: "+ index);
							
							byte[] buf = new byte[PieceSize];
							buf = readfile(index);
							if(!chokeOther[peerID - 1001]){
								PieceMessage pieceMessage = new PieceMessage(PieceSize, (byte)7, m.payload, buf);
								
								send(peerID, pieceMessage);
								//System.out.println("Peer [" + MyID + "] sent a piece to Peer [" + peerID + "] which is piece " + index);
								log("Peer [" + MyID + "] sent a piece to Peer [" + peerID + "] which is piece " + index);
							}
							else{
								Message refuseMessage = new Message(4, (byte)8, m.payload);
								send(peerID, refuseMessage);
								log("Peer [" + MyID + "] refused to send a piece to Peer [" + peerID + "] which is piece " + index);
							}
						}
						else if(m.type == (byte) 4){
							int index = byteArrayToInt(m.payload);
							//System.out.println("received have message from peer" + peerID +"for the piece[" + index +"]");
							log("Peer [" + MyID + "] received have message from Peer [" + peerID + "] for the piece [" + index +"]");
							PeerPiece[peerID - 1001].set(index);
							int wantNum = comparePiece(MyID, peerID, PieceNum);
							Message interesting;
							if(wantNum!=-1){
								interesting = new Message(0, (byte)2, null);
								log("Peer [" + MyID + "] sent the 'interested' message to Peer [" + peerID + "]");
							}
							else{
								interesting = new Message(0, (byte)3, null);
								log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + peerID + "]");
							}
							send(peerID, interesting);	
						}
						else if(m.type == (byte)8){
							int index = byteArrayToInt(m.payload);
							System.out.println("Peer " + peerID + " refuse send piece [" + index +"]");
							log("Peer [" + MyID + "] received refuse message from Peer [" + peerID + "] for the piece [" + index +"]");
							requestSent[index] = false;
						}
						else if(m.type == (byte)9){
							System.out.println("received complete message from Peer" + peerID);
							peerComplete[peerID - 1001] = true;
							log("Peer [" + MyID + "] received complete message from Peer [" + peerID + "]");
							finish();
						}
						else{
							//System.out.println("Peer[" + MyID +" received wrong message from Peer [" + peerID + "]");
						}
						
					}
					else if(receivedMessage instanceof PieceMessage)
					{
						PieceMessage m = (PieceMessage) receivedMessage;
						if(m.type == (byte)7)
						{
							peerRate[peerID-1001]++;
							int index = byteArrayToInt(m.index);
							byte[] buf = new byte[PieceSize];
							buf = m.payload;
							
							DataOutputStream fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(ClientFilepath +  fileName + index + ".part")));
							
							fileOut.write(buf);
							
							fileOut.close();
							
							log("Peer [" + MyID + "] has downloaded the piece [" + index + "] from Peer [" + peerID + "]");
							//System.out.println("Peer [" + MyID + "] has downloaded the piece [" + index + "] from Peer [" + peerID + "]");
							//System.out.println("file piece index : "+index);
							//System.out.println("MyID : "+MyID);
							//GENG XIN PEERPIECE SHU ZU
							PeerPiece[MyID - 1001].set(index);
							//System.out.println("Peer [" + MyID + "] Send have message to all other peers");
							log("Peer [" + MyID + "] sent have [" + index + "] message to all Peers");
							sendhave(PeerNum, MyID, m.index);
							
							for(int i = 0; i<PeerNum; i++){
								if((i+1001) != MyID && (i+1001) != peerID){
									int interestIndex = comparePiece(MyID, i+1001, PieceNum);
									Message interesting;

									if(interestIndex == -1){
										interesting = new Message(0, (byte)3, null);
										log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + (i+1001) + "]");
										send(i+1001, interesting);
										//System.out.println("After received piece, send 'not interested' to Peer" + (i+1001) + "]");;
									}									
								}
							}
							
							int Complete = comparePiece(MyID, 1001, PieceNum);
							System.out.println("the result of compare after received piece: "+Complete);
							if(Complete == -1){
								FileProcess fileprocess = new FileProcess(fileName, ClientFilepath, ClientFilepath, PieceSize);
								try {
									fileprocess.reassemble(PieceNum);
									sendComplete(PeerNum, MyID);
									log("Peer [" + MyID + "] has downloaded the complete file");
									peerComplete[MyID-1001] = true;
									System.out.println("My file is completed.");
									finish();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
							int wantNum = comparePiece(MyID, peerID, PieceNum);
							if(wantNum == -1){
								Message interesting;
								interesting = new Message(0, (byte)3, null);
								log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + peerID + "]");
								send(peerID, interesting);
								//System.out.println("After received piece, send 'not interested' to Peer" + peerID + "]");;
							}
							else{
								if(!chokeMe[peerID - 1001] && !requestSent[wantNum]){
									System.out.println("send request for piece index: " + wantNum);
									byte[] requestBytes = intToByteArray(wantNum);
									Message requestMessage = new Message(4, (byte)6, requestBytes);
									send(peerID, requestMessage);
									requestSent[wantNum] = true;
									log("Peer [" + MyID + "] send request message to Peer [" + peerID + "] for piece [" + wantNum +"]" );
									//System.out.println("After received piece, send request Message to Peer" + peerID + "]");
								}
							}
							
							
						}
					}

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

	}
	
	public class Server extends Thread{
		private Socket connection; // the socket connection used in this client
	    private ObjectInputStream read; // stream read from socket		
		//private ObjectOutputStream write;    //stream write to the socket
		
		private int PeerID;
		private int MyID;
		
		//public int[] piece;
		public boolean[] interest;
		private BitSet[] PeerPiece;
		
		public Server(Socket connection, int MyID,  boolean[] interest, BitSet[] PeerPiece, int peerID){
			this.connection = connection;
			this.MyID = MyID;
			this.interest = interest;
			this.PeerPiece = PeerPiece;
			this.PeerID = peerID;
		}
		
		public void run(){
			System.out.println("New client is comming!");
			try {
				read = new ObjectInputStream(connection.getInputStream());
				while(true){
					Object receivedMessage = read.readObject();
					if(receivedMessage instanceof HandShake){
						System.out.println("HandShake message from peer : "+PeerID );
						log("Peer [" + MyID + "] makes a connection to Peer [" + PeerID + "]");
						HandShake shakeHand = new HandShake(MyID);
						send(PeerID, shakeHand);
						log("Peer [" + MyID + "] is connected from Peer [" + PeerID + "]");
						System.out.println("my bitfield ready to send:" + PeerPiece[MyID-1001]);
						byte[] bytepayload = BitSettoByteArray( PeerPiece[MyID-1001], PieceNum);
						//System.out.println("my bitfiled converse back" + fromByteArray(bytepayload, PieceNum, MyID).toString());
						
						
						Message bitFiledMessage = new Message(bytepayload.length, (byte)5, bytepayload);
						send(PeerID,bitFiledMessage);
						log("Peer [" + MyID + "] sent 'bitfield' message to Peer [" + PeerID + "]");
					}
					else if(receivedMessage instanceof Message){
						Message m = (Message) receivedMessage;
						if( m.type == (byte)5){
							System.out.println("bitfiled message received from " + PeerID);
							log("Peer [" + MyID + "] received the 'bitfield' message from Peer [" + PeerID + "]");
							fromByteArray(m.payload,PieceNum, PeerID);
							System.out.println("peerPiece bitfield:  "+PeerPiece[PeerID - 1001].toString());
							int wantNum = comparePiece(MyID, PeerID, PieceNum);
							Message interesting;
							if(wantNum!=-1){
								interesting = new Message(0, (byte)2, null);
								log("Peer [" + MyID + "] sent the 'interested' message to Peer [" + PeerID + "]");
							}
							else{
								interesting = new Message(0, (byte)3, null);
								log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + PeerID + "]");
							}
							send(PeerID, interesting);	
						}
						else if(m.type == (byte)2){
							log("Peer [" + MyID + "] received 'interested' message from Peer [" + PeerID + "]");
							interest[PeerID - 1001] = true;
							//System.out.println("Received interest");
						}
						else if(m.type == (byte)3){
							log("Peer [" + MyID + "] received 'not interested' message from Peer [" + PeerID + "]");
							interest[PeerID - 1001] = false;
							//System.out.println("Received not interest");
						}
						else if(m.type == (byte)1){
							log("Peer [" + MyID + "] is unchoked by Peer [" + PeerID + "]");
							chokeMe[PeerID - 1001] = false;
							int index = comparePiece(MyID, PeerID, PieceNum);
							if(index != -1 && !requestSent[index]){
								System.out.println("send request for piece index: " + index);
								byte[] indexBytes = intToByteArray(index);
								Message requestMessage = new Message(4, (byte)6, indexBytes);
								send(PeerID, requestMessage);
								requestSent[index] = true;
								log("Peer [" + MyID + "] send request message to Peer [" + PeerID + "] for piece [" + index +"]" );
							}
							else{
								//System.out.println("Unchoke from not interested peer" + PeerID);
							}
						}
						else if(m.type == (byte)0){
							log("Peer [" + MyID + "] is choked by Peer [" + PeerID + "]");
							chokeMe[PeerID - 1001] = true;
						}
						else if(m.type == (byte)6){
							log("Peer [" + MyID + "] received request from Peer [" + PeerID + "]");
							int index = byteArrayToInt(m.payload);
							//System.out.println("request from Peer "+ PeerID +" of piece index: "+ index);
							
							byte[] buf = new byte[PieceSize];
							buf = readfile(index);
							
							if(!chokeOther[PeerID - 1001]){
								PieceMessage pieceMessage = new PieceMessage(PieceSize, (byte)7, m.payload, buf);								
								send(PeerID, pieceMessage);
								//System.out.println("Peer [" + MyID + "] sent a piece to Peer [" + PeerID + "] which is piece " + index);
								log("Peer [" + MyID + "] sent a piece to Peer [" + PeerID + "] which is piece " + index);
							}
							else{
								Message refuseMessage = new Message(4, (byte)8, m.payload);
								send(PeerID, refuseMessage);
								log("Peer [" + MyID + "] refused to send a piece to Peer [" + PeerID + "] which is piece " + index);
							}
							
						}
						else if(m.type == (byte)4){
							int index = byteArrayToInt(m.payload);
							//System.out.println("received have message from peer" + PeerID +"for the piece[" + index +"]");
							log("Peer [" + MyID + "] received have message from Peer [" + PeerID + "] for the piece [" + index +"]");
							PeerPiece[PeerID - 1001].set(index);
							int wantNum = comparePiece(MyID, PeerID, PieceNum);
							Message interesting;
							if(wantNum!=-1){
								interesting = new Message(0, (byte)2, null);
								log("Peer [" + MyID + "] sent the 'interested' message to Peer [" + PeerID + "]");
							}
							else{
								interesting = new Message(0, (byte)3, null);
								log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + PeerID + "]");
							}
							send(PeerID, interesting);
						}
						else if(m.type == (byte)8){	
							int index = byteArrayToInt(m.payload);
							System.out.println("Peer " + PeerID + " refuse send piece [" + index +"]");
							log("Peer [" + MyID + "] received refuse message from Peer [" + PeerID + "] for the piece [" + index +"]");
							requestSent[index] = false;
						}
						else if(m.type == (byte)9){
							System.out.println("received complete message from Peer" + PeerID);
							peerComplete[PeerID - 1001] = true;
							log("Peer [" + MyID + "] received complete message from Peer [" + PeerID + "]");
							finish();
						}
						else{
							System.out.println("Peer[" + MyID +" received wrong message from Peer [" + PeerID + "]");
						}
						
					}
					else if(receivedMessage instanceof PieceMessage)
					{
						PieceMessage m = (PieceMessage) receivedMessage;
						if(m.type == (byte)7)
						{
							int index = byteArrayToInt(m.index);
							
							byte[] buf = new byte[PieceSize];
							buf = m.payload;
							
							DataOutputStream fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(ClientFilepath +  fileName + index + ".part")));
							
							fileOut.write(buf);
							
							log("Peer [" + MyID + "] has downloaded the piece [" + index + "] from Peer [" + PeerID + "]");
							//System.out.println("Peer [" + MyID + "] has downloaded the piece [" + index + "] from Peer [" + PeerID + "]");
							
							fileOut.close();
							
							//GENG XIN PEERPIECE SHU ZU
							PeerPiece[MyID - 1001].set(index);
							//System.out.println("Peer [" + MyID + "] Send have message to all other peers");
							log("Peer [" + MyID + "] sent have [" + index + "] message to all Peers");
							sendhave(PeerNum, MyID, m.index);	
							
							for(int i = 0; i<PeerNum; i++){
								if((i+1001) != MyID && (i+1001) != PeerID){
									int interestIndex = comparePiece(MyID, i+1001, PieceNum);
									Message interesting;

									if(interestIndex == -1){
										interesting = new Message(0, (byte)3, null);
										log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + (i+1001) + "]");
										send(i+1001, interesting);
										//System.out.println("After received piece, send 'not interested' to Peer" + (i+1001) + "]");;
									}									
								}
							}
							
							int Complete = comparePiece(MyID, 1001, PieceNum);
							System.out.println("the result of compare after received piece: "+Complete);
							if(Complete == -1){
								FileProcess fileprocess = new FileProcess(fileName, ClientFilepath, ClientFilepath, PieceSize);
								try {
									fileprocess.reassemble(PieceNum);
									sendComplete(PeerNum, MyID);
									log("Peer [" + MyID + "] has downloaded the complete file");
									peerComplete[MyID-1001] = true;
									System.out.println("My file is completed.");
									finish();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
							int wantNum = comparePiece(MyID, PeerID, PieceNum);
							if(wantNum == -1){
								Message interesting;
								interesting = new Message(0, (byte)3, null);
								log("Peer [" + MyID + "] sent the 'not intereted' message to Peer [" + PeerID + "]");
								send(PeerID, interesting);
							//	System.out.println("After received piece, send 'not interested' to Peer" + PeerID + "]");;
							}
							else{
								if(!chokeMe[PeerID - 1001] && !requestSent[wantNum]){
									System.out.println("send request for piece index: " + wantNum);
									byte[] requestBytes = intToByteArray(wantNum);
									Message requestMessage = new Message(4, (byte)6, requestBytes);
									send(PeerID, requestMessage);
									requestSent[wantNum] = true;
									//System.out.println("After received piece, send request Message to Peer" + PeerID + "]");
									log("Peer [" + MyID + "] send request message to Peer [" + PeerID + "] for piece [" + wantNum +"]" );
								}
							}
						}
					}
					else
					{
						System.out.println("Received Message ERROR.");
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		

		
	}
	
	public class Choke extends Thread{
		
		public void run(){
			int count = 0;
			
			while(true){
				//System.out.println("after while true");
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				count++;
				if(count%unchokeInterval == 0){
					//determine choke
					List<Integer> maxIndex = selectMaxrate();
					System.out.println("List maxIndex size = " + maxIndex.size() );
					String preferIDList="";
					for(int i = 0; i < PeerNum; i++){
						if(chokeOther[i]){
							if(maxIndex.contains(i)){
								chokeOther[i] = false;
								Message unchokeMessage = new Message(0, (byte)1, null);
								send((i+1001), unchokeMessage);
								log("Peer [" + MyID + "] unchoke Peer [" + (i+1001) + "]");
								System.out.println("Send unchoke to peer" + (i+1001));
							}
						}
						else{
							if(!maxIndex.contains(i) && optimisticUnchoke != i){
								chokeOther[i] = true;
								Message chokeMessage = new Message(0, (byte)0, null);
								send((i+1001), chokeMessage);
								log("Peer [" + MyID + "] choke Peer [" + (i+1001) + "]");
								System.out.println("Send choke to peer" + (i+1001));
							}
						}
					}
					
					for(int i = 0; i < maxIndex.size(); i++){
						int pID = maxIndex.get(i)+ 1001;
						preferIDList = preferIDList + pID + ",";
						System.out.println("preferIDList after i " + i +" loop is " + preferIDList);
					}	
					System.out.println("preferIDList" + preferIDList);
					
					if(!preferIDList.equals("")){
						log("Peer [" + MyID + "] has the preferred neighbors [" + preferIDList.substring(0,preferIDList.length() - 1) + "]");					
					}
					for(int i = 0; i < PeerNum; i++){
						peerRate[i] = 0;
					}
				}
				if(count%optimisticUnchokingInterval == 0){
					List<Integer> chokeAndInterest = new LinkedList<Integer>();
					for(int i = 0; i<PeerNum; i++){
						if(interest[i]&&chokeOther[i] ){
							chokeAndInterest.add(i);
						}
					}
					if(chokeAndInterest.size() > 0)
					{
						int unchokeNum = 0;
						for(int i = 0; i < PeerNum; i++){
							if(!chokeOther[i])
								unchokeNum++;
						}
						if(unchokeNum >= (numberOfPreferredNeighbors + 1)){
							chokeOther[optimisticUnchoke] = true;
							Message chokeMessage = new Message(0, (byte)0, null);
							send(optimisticUnchoke + 1001, chokeMessage);
							log("Peer [" + MyID + "] choke Peer [" + (optimisticUnchoke+1001) + "]");
							System.out.println("last optimisticUnchoke is choked " + (optimisticUnchoke + 1001));						
						}
						Random rand = new Random();
						optimisticUnchoke = chokeAndInterest.get(rand.nextInt(chokeAndInterest.size()));
						System.out.println("current optimisticUnchoke is " + (optimisticUnchoke + 1001));
						chokeOther[optimisticUnchoke] = false;
						Message unchokeMessage = new Message(0, (byte)1, null);
						send((optimisticUnchoke+1001), unchokeMessage);
						log("Peer [" + MyID + "] has the optimistically unchoked neighbor [" + (optimisticUnchoke+1001) + "]");
					}
				}
			}
		}
		
		public List<Integer> selectMaxrate(){
			List<Integer> maxIndex = new LinkedList<Integer>();
			int max = -1;
			int indexPotent = -1;
			for(int i = 0; i < numberOfPreferredNeighbors; i++){
				for(int j=0;j<PeerNum;j++){
					if(interest[j] && peerRate[j] > max){
						max = peerRate[j];
						indexPotent = j;
					}
					else if(interest[j] && peerRate[j] == max && peerRate[j] != -1){
						Random rand = new Random();
						int Num = rand.nextInt(2);
						if(Num == 1){
							max = peerRate[j];
							indexPotent = j;
						}
					}
				}
				if(indexPotent!=-1){
					maxIndex.add (indexPotent);
					peerRate[indexPotent] = -1;
				}
				max = -1;
				indexPotent=-1;
			}
			return maxIndex;
		}

	}

}
