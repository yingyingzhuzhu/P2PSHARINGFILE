package P2PFileSharing;

import java.io.*;
import java.util.*;

import FileProcess.FileProcess;

public class peerProcess {
	
	static String fileDir = System.getProperty("user.dir");
	static int port;
	static String[][] IDIPport = new String[20][4];
	static Hashtable<String, Integer> IPID = new Hashtable<String, Integer>();
	static int NumberOfPreferredNeighbors;
	static int UnchokingInterval;
	static int OptimisticUnchokingInterval;
	static String FileName;
	static int FileSize;
	static int PieceSize;
	static int PieceNum;
	static int PeerNum;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int peerID;
		peerID = Integer.parseInt("1002");	
		getPathandPort(fileDir);
		peerConfig(fileDir);
		System.out.println("pieceNum = : "+PieceNum);
		extractIPID();
			String ClientFilepath = fileDir + "/peer_"+ peerID + "/";
			port =Integer.parseInt(IDIPport[0][2]);
			
			if(peerID == 1001)
			{
				FileProcess fileprocess = new FileProcess(FileName, ClientFilepath, ClientFilepath, PieceSize);
				try {
					fileprocess.split();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			
			Peer peer = new Peer(peerID, port, fileDir, PieceNum, IDIPport, IPID, PeerNum, UnchokingInterval, OptimisticUnchokingInterval, NumberOfPreferredNeighbors, FileName, ClientFilepath, PieceSize);
			peer.run();
			System.out.println("Peer process is terminated, Victory!");
	}
	
	static public void extractIPID(){
		//int ID0 = Integer.parseInt(IDIPport[0][0]) - 1001;
		//IPID.put(IDIPport[0][1], ID0);
		for(int i = 0 ; i < PeerNum; i++){
			IPID.put(IDIPport[i][1], Integer.parseInt(IDIPport[i][0]));
		}
		System.out.println("extractIPID sucessfully, PeerNum: " + PeerNum);
	}
	
	static public void getPathandPort(String filePath){
		try{
			FileReader newFileReader = new FileReader(filePath + "/PeerInfo.cfg"); 
			BufferedReader BR = new BufferedReader(newFileReader); 
			
			String str = null;
			int number = 0;
			while((str = BR.readLine())!= null){
				String[] splited = str.split("\\s+");
				for(int j = 0; j < 4; j++){
					IDIPport[number][j] = splited[j];
				}
				number++;
			}
			PeerNum = number;
			BR.close();
			newFileReader.close();	
			System.out.println("read successfuly1");
		}
		catch(Exception e){
			System.err.println("Error happens when read file1.");
		}
	}
	
	static public void peerConfig(String filePath){
		try{
			FileReader newFileReader = new FileReader(filePath + "/Common.cfg"); 
			BufferedReader BR = new BufferedReader(newFileReader); 
			
			String str = null;
			
			str = BR.readLine();
			String[] splited = str.split("\\s+");
			NumberOfPreferredNeighbors = Integer.parseInt(splited[1]); 
			str = BR.readLine();
			splited = str.split("\\s+");			
			UnchokingInterval = Integer.parseInt(splited[1]);
			str = BR.readLine();
			splited = str.split("\\s+");			
			OptimisticUnchokingInterval = Integer.parseInt(splited[1]);	
			str = BR.readLine();
			splited = str.split("\\s+");			
			FileName = splited[1];
			str = BR.readLine();
			splited = str.split("\\s+");			
			FileSize = Integer.parseInt(splited[1]);
			str = BR.readLine();
			splited = str.split("\\s+");			
			PieceSize = Integer.parseInt(splited[1]);
			
			PieceNum = (FileSize + PieceSize - 1)/PieceSize;
			
			BR.close();
			newFileReader.close();	
			System.out.println("read successfuly2");
		}
		catch(Exception e){
			System.err.println("Error happens when read file2.");
		}
	}

}
