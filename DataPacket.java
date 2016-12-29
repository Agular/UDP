
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
		if (bytesToBeRead != 0) {
			this.data = new byte[bytesToBeRead];
			System.arraycopy(data, 3, this.data, 0, bytesToBeRead);
		} else{
			crc = ((data[3] & 0xFF) << 24) | ((data[4] & 0xFF) << 16) | ((data[5] & 0xFF) << 8)
					| (data[6] & 0xFF);
		}
	}

	@Override
	byte[] returnData() {
		if (crc == -1) {
			int dataLength = this.data.length;
			byte[] data = new byte[dataLength + 3];
			data[0] = (byte) (sessionNr >> 8);
			data[1] = (byte) (sessionNr & 0xFF);
			data[2] = (byte) packetNr;
			System.arraycopy(this.data, 0, data, 3, dataLength);
			return data;
		} else if (crc != -1 && this.data != null) {
			int dataLength = this.data.length;
			int sendPacketLength = dataLength + 7;
			byte[] data = new byte[sendPacketLength];
			data[0] = (byte) (sessionNr >> 8);
			data[1] = (byte) (sessionNr & 0xFF);
			data[2] = (byte) packetNr;
			System.arraycopy(this.data, 0, data, 3, dataLength);
			int iCrc = (int) crc;
			data[sendPacketLength - 4] = (byte) (iCrc >> 24);
			data[sendPacketLength - 3] = (byte) (iCrc >> 16);
			data[sendPacketLength - 2] = (byte) (iCrc >> 8);
			data[sendPacketLength - 1] = (byte) (iCrc /* >> 0 */);
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
