
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.zip.CRC32;

public class Server {
	static short sessionNr = -1;;
	static int packetNr;
	static String outFileName;
	static byte[] data;
	static long totalBytesRead;
	static int bufferSize = 1500;
	static long fileSize;
	static int bytesToBeRead;
	static FileOutputStream outPutStream = null;
	static final int START_PACKET = 0;
	static final int DATA_PACKET = 1;
	static final int BROKEN_PACKET = -1;
	static CRC32 crc32 = null;
	static boolean lastCrcIsReceived;
	static File outFile = null;

	public static void main(String[] args) {
		int port;
		DatagramSocket socket = null;

		byte[] buf = new byte[bufferSize];
		Ack ack = null;
		DatagramPacket ackDatagrampacket = null;

		/*
		 * Check for port input. Exit if failed.
		 */
		if (args.length != 1) {
			System.out.println("Input: port");
			System.exit(-1);
		}
		port = Integer.parseInt(args[0]);
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			System.out.println("Cannot open port on this number.");
			e.printStackTrace();
			System.exit(-1);
		}
		// Create a datagram packet to hold incoming UDP startPacket.
		DatagramPacket inPacket = new DatagramPacket(buf, bufferSize);
		// Block until the host receives a UDP packet.
		while (!socket.isClosed()) {
			// Create a datagram packet to hold incoming UDP packet.
			inPacket = new DatagramPacket(buf, bufferSize);
			// Block until the host receives a UDP packet.
			try {
				socket.receive(inPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Determine packet type
			switch (determinePacket(inPacket)) {
			case START_PACKET:
				// The client has not received any startpackets or the client
				// has finished it's last receive.
				if (sessionNr == -1 || (fileSize != 0 && fileSize == totalBytesRead)) {
					System.out.println("SERVER: RECEIVED START PACKET");
					try {
						readStartData(inPacket);
						crc32 = new CRC32();
						lastCrcIsReceived = false;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				createFile();
				ack = new Ack(sessionNr, packetNr);
				ackDatagrampacket = new DatagramPacket(ack.returnData(), 3, inPacket.getAddress(), inPacket.getPort());
				try {
					System.out.println("SERVER: SENDING ACK SESSION: " + sessionNr + " PACKET: " + packetNr + "\n");
					socket.send(ackDatagrampacket);
				} catch (IOException e) {
					System.out.println("Could not send ACK!");
					e.printStackTrace();
				}
				break;
			case DATA_PACKET:
				System.out.println("SERVER: RECEIVED DATA PACKET");
				// Print the receieved data.
				if (isNewDataPacket(inPacket)) {
					try {
						readDataPacket(inPacket);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				ack = new Ack(sessionNr, packetNr);
				ackDatagrampacket = new DatagramPacket(ack.returnData(), 3, inPacket.getAddress(), inPacket.getPort());
				try {
					System.out.println("SERVER: SENDING ACK SESSION: " + sessionNr + " PACKET: " + packetNr + "\n");
					socket.send(ackDatagrampacket);
				} catch (IOException e) {
					System.out.println("Could not send ACK!");
					e.printStackTrace();
				}
				break;
			case BROKEN_PACKET:
				System.out.println("SERVER: RECEIVED BROKEN (WRONG PROTOCOL?) PACKET - NO REPLY SENT");
				break;
			}
		}

	}

	private static void readStartData(DatagramPacket request) throws Exception {
		// Obtain references to the packet's array of bytes.
		StartPacket startPacket = new StartPacket(request.getData());
		sessionNr = startPacket.getSessionNr();
		packetNr = startPacket.getPacketNr();
		outFileName = startPacket.getFileName();
		fileSize = startPacket.getFileSize();
		totalBytesRead = 0;
		System.out.println("SERVER: Received from " + request.getAddress().getHostAddress() + ": SESSIONR: "
				+ startPacket.sessionNr + " PACKET: " + startPacket.getPacketNr());
		System.out.println("SERVER: Receiving file: " + startPacket.getFileName() + " size: "
				+ startPacket.getFileSize() + " bytes");
		System.out.println("SERVER: Included CRC32 checksum is: " + startPacket.getCrc() + "\n");
	}

	private static void readDataPacket(DatagramPacket request) throws Exception {
		// Obtain references to the packet's array of bytes.
		byte[] requestedData = request.getData();
		int packetSize = request.getLength();
		DataPacket dataPacket;
		System.out.println("THE SIZE OF RECEIVED DATAPACKET: " + packetSize);
		// If all written data is read and crc is left.
		if (totalBytesRead == fileSize) {
			System.out.println("SERVER: LAST DATA PACKET ONLY CRC");
			bytesToBeRead = 0;
			dataPacket = new DataPacket(requestedData, bytesToBeRead, false);
			lastCrcIsReceived = true;
		}
		// If the packet is not the last one...
		else if (totalBytesRead + packetSize - 7 < fileSize) {
			System.out.println("SERVER: NORMAL DATA PACKET");
			bytesToBeRead = packetSize - 3;
			dataPacket = new DataPacket(requestedData, bytesToBeRead, false);
			// The packet is last one.
		} else if (totalBytesRead + packetSize - 7 == fileSize) {
			System.out.println("SERVER: LAST DATA PACKET");
			bytesToBeRead = packetSize - 7;
			dataPacket = new DataPacket(requestedData, bytesToBeRead, true);
			lastCrcIsReceived = true;
			// The packet is last but without crc
		} else {
			System.out.println("SERVER: LAST DATA PACKET (WITHOUT CRC)");
			bytesToBeRead = (int) (fileSize - totalBytesRead);
			System.out.println(bytesToBeRead);
			dataPacket = new DataPacket(requestedData, bytesToBeRead, false);
		}
		sessionNr = dataPacket.getSessionNr();
		packetNr = dataPacket.getPacketNr();
		data = dataPacket.returnSendData();
		System.out.println("SERVER: DATAPACKET RECEIVE " + request.getAddress().getHostAddress() + ": SESSIONR: "
				+ dataPacket.getSessionNr() + " PACKET: " + dataPacket.getPacketNr() + "\n");
		if (fileSize != totalBytesRead) {
			outPutStream.write(data);
			totalBytesRead += bytesToBeRead;
			crc32.update(data);
			System.out.println(fileSize + " " + totalBytesRead);
			if (fileSize == totalBytesRead) {
				outPutStream.close();
			}
		}
		if (lastCrcIsReceived) {
			System.out.println(dataPacket.getICrc() + " " + (int) crc32.getValue() );
			checkFileIntegrity(dataPacket.getICrc(), (int) crc32.getValue());
		}
	}

	private static boolean isStartPacketValid(DatagramPacket request) {
		byte[] startPacketData = request.getData();
		// First let's check if it has "Start" in it's package.
		byte[] tempStart = new byte[5];
		System.arraycopy(startPacketData, 3, tempStart, 0, 5);
		if ((int) startPacketData[2] != 0 || !new String(tempStart).equals("Start")) {
			return false;
		}
		short fileNameSize = (short) (((startPacketData[16] & 0xFF) << 8) | (startPacketData[17] & 0xFF));
		if (22 + fileNameSize != request.getLength()) {
			return false;
		}
		// Now check if startPacket crc is created correctly.
		int lStartPacketData = startPacketData.length;
		CRC32 crc32 = new CRC32();
		// Get the packet CRC. We need fileNameSize because the input byte array
		// is with default size of 1500.
		int crcIdx = 18 + fileNameSize;
		System.out.println("crcIdx " + crcIdx + " " + lStartPacketData);
		int packetCrc = ((startPacketData[crcIdx] & 0xFF) << 24) | ((startPacketData[crcIdx + 1] & 0xFF) << 16)
				| ((startPacketData[crcIdx + 2] & 0xFF) << 8) | (startPacketData[crcIdx + 3] & 0xFF);
		// Create the crc on our own.
		crc32.update(startPacketData, 0, crcIdx);
		// Check the int values of the crc.
		if ((int) crc32.getValue() != packetCrc) {
			return false;
		}
		return true;
	}

	private static void createFile() {
		try {
			outFile = new File(outFileName);
			if (!outFile.exists()) {
				outFile.createNewFile();
			} else {
				for (int i = 1; i < Integer.MAX_VALUE; i++) {
					outFile = new File(i + outFileName);
					if (!outFile.exists()) {
						break;
					}
				}
			}
			outPutStream = new FileOutputStream(outFile);
		} catch (IOException e1) {
			;
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private static int determinePacket(DatagramPacket request) {
		if (request.getLength() >= 18) {
			if (isStartPacketValid(request)) {
				return START_PACKET;
			}
		}
		if (sessionNr != -1) {
			return DATA_PACKET;
		}
		return BROKEN_PACKET;
	}

	private static boolean isNewDataPacket(DatagramPacket dataPacket) {
		byte[] temp = dataPacket.getData();
		if ((int) temp[2] != packetNr) {
			return true;
		}
		return false;
	}

	private static void checkFileIntegrity(int clientCrc, int serverCrc) {
		// TODO Auto-generated method stub
		if (clientCrc == serverCrc) {
			System.out.println("SERVER: FILE WAS SUCCESSFULLY RECEIVED");
			System.out.println("SERVER: WAITING FOR NEXT FILE!\n");
		} else {
			System.out.println("SERVER: FILE RECEIVE WAS UNSUCCESSFUL");
			System.out.println("SERVER: DELETING FILE: " + outFileName);
			outFile.delete();
		}
	}
}
