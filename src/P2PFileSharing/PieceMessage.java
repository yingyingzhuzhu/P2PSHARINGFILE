package P2PFileSharing;

import java.io.Serializable;

public class PieceMessage implements Serializable{
	
	int length;
	byte type;
	byte[] index;
	byte[] payload;

	public PieceMessage(int length, byte type, byte[] index, byte[] payload){
		this.length = length;
		this.type = type;
		this.payload = payload;
		this.index = index;
	}

}
