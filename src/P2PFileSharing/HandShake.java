package P2PFileSharing;

import java.io.*;

public class HandShake  implements Serializable {
	String header = "P2PFILESHARINGPROJ";
	byte[] zero = new byte[10];
	int peerID;
	public HandShake(int peerID){
		this.peerID = peerID;
		for(int i = 0; i<10; i++){
			zero[i] = 0;
		}
	}
}
