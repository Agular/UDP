
public class DataPacket extends Packet {
	byte[] data;
	int crc = 0;

	public DataPacket(short sessionNr, int packetNr, byte[] data, int bytesRead) {
		super.sessionNr = sessionNr;
		super.packetNr = packetNr;
		if (data.length == bytesRead) {
			this.data = data;
		} else if (data.length - bytesRead >= 3) {
			this.data = new byte[bytesRead];
			System.arraycopy(data, 0, this.data, 0, bytesRead);
		} else if (data.length - bytesRead < 3) {
			System.out.println("Cant fit all of last read buffer to packet!!");
		}
	}

	public DataPacket(byte[] data, int bytesToBeRead) {
		super.sessionNr = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
		super.packetNr = (int) data[2];
		this.data = new byte[bytesToBeRead];
		System.arraycopy(data, 3, this.data, 0, bytesToBeRead);
		/*if(){
			crc = ((data[26] & 0xFF) << 24) | ((data[27] & 0xFF) << 16)
					| ((data[28] & 0xFF) << 8) | (data[29] & 0xFF);
		}**/
	}

	@Override
	byte[] returnData() {
		byte[] data = new byte[this.data.length + 3];
		data[0] = (byte) (sessionNr >> 8);
		data[1] = (byte) (sessionNr & 0xFF);
		data[2] = (byte) packetNr;
		System.arraycopy(this.data, 0, data, 3, this.data.length);
		return data;
	}

	byte[] returnSendData() {
		return this.data;
	}

	public int getCrc(){
		if(crc!=0){
		return crc;
		} else {
			return -1;
		}
	}
}
