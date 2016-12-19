import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class StartPacket extends Packet {
	// sessionNr - 2 Bytes
	// packetNr - 1 Byte
	String start = "Start"; // 5 Bytes
	long fileSize; // 64 bit unsigned int (8 Bytes)
	short fileNameSize; // 2 Bytes
	String fileName; // 0-255 Bytes
	int crc; // 32 bit (4 Byes)

	/*
	 * Used for creating the startPacket. Used in client.
	 * Our byte[] data must have the size (22 + fileNameSize) bytes
	 */
	public StartPacket(short sessionNr, int packetNr, String fileName, long fileSize) {
		super.sessionNr = sessionNr;
		super.packetNr = packetNr;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.fileNameSize = (short) this.fileName.length();
	}
	/*
	 * Rebuilds the startPacket. Used in server.
	 * */
	public StartPacket(byte[] data) {
		super.sessionNr = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
		super.packetNr = (int) data[2];
		// get Start, not
		// necessary atm, maybe in the future
		byte[] test = Arrays.copyOfRange(data, 3, 8);
		// System.out.println(new String(test));
		fileSize = 0;
		for (int i = 0; i < 8; i++) {
			fileSize <<= 8;
			fileSize |= (data[8 + i] & 0xFF);
		}
		fileNameSize = (short) (((data[16] & 0xFF) << 8) | (data[17] & 0xFF));
		fileName = new String(Arrays.copyOfRange(data, 18, 18 + fileNameSize));

		crc = ((data[26] & 0xFF) << 24) | ((data[27] & 0xFF) << 16)
				| ((data[28] & 0xFF) << 8) | (data[29] & 0xFF);
	}
	/*
	 * Used in client to create the packet for the startPacket.
	 * */
	@Override
	byte[] returnData() {
		byte[] data = new byte[22 + fileNameSize];
		int dataLength = data.length;
		data[0] = (byte) (sessionNr >> 8);
		data[1] = (byte) (sessionNr & 0xFF);
		data[2] = (byte) packetNr;
		System.arraycopy(start.getBytes(), 0, data, 3, 5); // Write Start
		long tempSize = fileSize;
		for (int i = 7; i >= 0; --i) {
			data[8 + i] = (byte) (tempSize & 0xff);
			tempSize >>= 8;
		}
		data[16] = (byte) (fileNameSize >> 8);
		data[17] = (byte) (fileNameSize & 0xFF);
		System.arraycopy(fileName.getBytes(), 0, data, 18, fileNameSize);
		CRC32 crc32 = new CRC32();
		crc32.update(data, 0, dataLength - 4);
		// System.out.println(String.format("0x%08X", (int)crc32.getValue()));
		crc = (int) crc32.getValue();
		data[dataLength - 4] = (byte) (crc >> 24);
		data[dataLength - 3] = (byte) (crc >> 16);
		data[dataLength - 2] = (byte) (crc >> 8);
		data[dataLength - 1] = (byte) (crc /* >> 0 */);
		return data;
	}

	public long getFileSize() {
		return fileSize;
	}

	public short getFileNameSize() {
		return fileNameSize;
	}

	public String getFileName() {
		return fileName;
	}

	public int getCrc() {
		return crc;
	}

}
