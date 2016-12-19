

public abstract class Packet {
	short sessionNr;
	int packetNr;
	byte[] data;
	
	public short getSessionNr() {
		return sessionNr;
	}
	public void setSessionNr(short sessionNr) {
		this.sessionNr = sessionNr;
	}
	public int getPacketNr() {
		return packetNr;
	}
	public void setPacketNr(int packetNr) {
		this.packetNr = packetNr;
	}
	abstract byte[] returnData();
}
