
public class DataPacket extends Packet {
	byte[] data = null;
	long crc = -1;

	public DataPacket(short sessionNr, int packetNr, byte[] data, int bytesToBeRead) {
		super.sessionNr = sessionNr;
		super.packetNr = packetNr;
		this.data = new byte[bytesToBeRead];
		System.arraycopy(data, 0, this.data, 0, bytesToBeRead);
	}

	public DataPacket(short sessionNr, int packetNr, byte[] data, int bytesToBeRead, long crc) {
		super.sessionNr = sessionNr;
		super.packetNr = packetNr;
		this.data = new byte[bytesToBeRead];
		System.arraycopy(data, 0, this.data, 0, bytesToBeRead);
		this.crc = crc;
	}

	public DataPacket(short sessionNr, int packetNr, long crc) {
		super.sessionNr = sessionNr;
		super.packetNr = packetNr;
		this.crc = crc;
	}

	public DataPacket(byte[] data, int bytesToBeRead) {
		super.sessionNr = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
		super.packetNr = (int) data[2];
		this.data = new byte[bytesToBeRead];
		System.arraycopy(data, 3, this.data, 0, bytesToBeRead);
	}

	@Override
	byte[] returnData() {
		if (crc == -1) {
			int dataLength = this.data.length;
			byte[] data = new byte[this.data.length + 3];
			data[0] = (byte) (sessionNr >> 8);
			data[1] = (byte) (sessionNr & 0xFF);
			data[2] = (byte) packetNr;
			System.arraycopy(this.data, 0, data, 3, this.data.length);
			return data;
		} else if (crc != -1 && this.data != null) {
			int dataLength = this.data.length;
			byte[] data = new byte[this.data.length + 7];
			data[0] = (byte) (sessionNr >> 8);
			data[1] = (byte) (sessionNr & 0xFF);
			data[2] = (byte) packetNr;
			System.arraycopy(this.data, 0, data, 3, this.data.length);
			int iCrc = (int) crc;
			data[dataLength - 4] = (byte) (iCrc >> 24);
			data[dataLength - 3] = (byte) (iCrc >> 16);
			data[dataLength - 2] = (byte) (iCrc >> 8);
			data[dataLength - 1] = (byte) (iCrc /* >> 0 */);
			return data;
		} else {
			byte[] data = new byte[7];
			data[0] = (byte) (sessionNr >> 8);
			data[1] = (byte) (sessionNr & 0xFF);
			data[2] = (byte) packetNr;
			int iCrc = (int) crc;
			data[3] = (byte) (iCrc >> 24);
			data[4] = (byte) (iCrc >> 16);
			data[5] = (byte) (iCrc >> 8);
			data[6] = (byte) (iCrc /* >> 0 */);
			return data;
		}
	}

	byte[] returnSendData() {
		return this.data;
	}

	int getDataSize() {
		if (data != null) {
			return this.data.length;
		} else {
			return -1;
		}
	}

	public long getCrc() {
		if (crc != 0) {
			return crc;
		} else {
			return -1;
		}
	}
	public int getICrc() {
		if (crc != 0) {
			return (int) crc;
		} else {
			return -1;
		}
	}
}
