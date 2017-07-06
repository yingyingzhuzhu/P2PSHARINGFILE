package P2PFileSharing;

import java.io.*;

public class Message implements Serializable{
	int length;
	byte type;
	byte[] payload;
	public Message(int length, byte type, byte[] payload){
		this.length = length;
		this.type = type;
		this.payload = payload;
	}
}
