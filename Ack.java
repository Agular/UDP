

public class Ack extends Packet {
	public Ack (short sessionNr, int packetNr){
		super.sessionNr = sessionNr;
		super.packetNr = packetNr;
	}
	public Ack(byte[] data){
		super.sessionNr = (short)( ((data[0]&0xFF)<<8) | (data[1]&0xFF) );
		super.packetNr = (int) data[2];
	}
	@Override
	byte[] returnData() {
		byte[] data = new byte[3];
		data[0] = (byte) (sessionNr >> 8);
		data[1] = (byte) (sessionNr & 0xFF);
		data[2] = (byte) packetNr;
		return data;
	}

}
