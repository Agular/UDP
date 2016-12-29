
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

public class Server {
	static short sessionNr;
	static int packetNr;
	static String outFileName;
	static byte[] data;
	static long totalBytesRead;
	static int bufferSize = 1500;
	static long fileSize;
	static int bytesToBeRead;

	public static void main(String[] args) {
		int port;
		DatagramSocket socket = null;
		File outFile;
		FileOutputStream outPutStream = null;
		byte[] buf = new byte[bufferSize];

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
		try {
			socket.receive(inPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Print the recieved data.
		try {
			readStartData(inPacket);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Ack ack = new Ack(sessionNr, packetNr);
		DatagramPacket ackDatagrampacket = new DatagramPacket(ack.returnData(), 3, inPacket.getAddress(),
				inPacket.getPort());
		try {
			outFile = new File(outFileName);
			if (!outFile.exists()) {
				outFile.createNewFile();
			} else {
				for (int i = 1; i < Integer.MAX_VALUE; i++) {
					outFile = new File(i+ outFileName);
					if(!outFile.exists()){
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

		try {
			System.out.println("SERVER: SENDING ACK: SESSIONNR: " + sessionNr + " PACKETNR: " + packetNr + "\n");
			socket.send(ackDatagrampacket);
		} catch (IOException e) {
			System.out.println("Could not send ACK!");
			e.printStackTrace();
		}

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
			// Print the recieved data.
			try {
				readDataPacket(inPacket);
				if (fileSize != totalBytesRead) {
					outPutStream.write(data);
					totalBytesRead += bytesToBeRead;
				} else {
					outPutStream.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		}

	}

	public static void readStartData(DatagramPacket request) throws Exception {
		// Obtain references to the packet's array of bytes.
		StartPacket startPacket = new StartPacket(request.getData());
		sessionNr = startPacket.getSessionNr();
		packetNr = startPacket.getPacketNr();
		outFileName = startPacket.getFileName();
		fileSize = startPacket.getFileSize();
		System.out.println("SERVER: Received from " + request.getAddress().getHostAddress() + ": SESSIONR: "
				+ startPacket.sessionNr + " PACKET: " + startPacket.getPacketNr());
		System.out.println("SERVER: Receiving file: " + startPacket.getFileName() + " size: "
				+ startPacket.getFileSize() + " bytes");
		System.out.println("SERVER: Included CRC32 checksum is: " + startPacket.getCrc() + "\n");
	}

	public static void readDataPacket(DatagramPacket request) throws Exception {
		// Obtain references to the packet's array of bytes.
		byte[] requestedData = request.getData();
		int packetSize = request.getLength();
		DataPacket dataPacket;
		System.out.println("THE SIZE OF RECEIVED DATAPACKET: " + packetSize);
		// If all written data is read and crc is left.
		if (totalBytesRead == fileSize) {
			System.out.println("SERVER: LAST DATA PACKET ONLY CRC");
			bytesToBeRead = 0;
			dataPacket = new DataPacket(requestedData, bytesToBeRead);
		}
		// If the packet is not the last one...
		else if (totalBytesRead + packetSize - 7 < fileSize) {
			System.out.println("SERVER: NORMAL DATA PACKET");
			bytesToBeRead = packetSize - 3;
			dataPacket = new DataPacket(requestedData, bytesToBeRead);
			// The packet is last one and entirely full.
		} else if (totalBytesRead + packetSize - 7 == fileSize) {
			System.out.println("SERVER: LAST DATA PACKET (FULL)");
			bytesToBeRead = packetSize - 7;
			dataPacket = new DataPacket(requestedData, bytesToBeRead);
			// The packet is last but not full
		} else {
			System.out.println("SERVER: LAST DATA PACKET (UNFULL)");
			bytesToBeRead = (int) (fileSize - totalBytesRead);
			System.out.println(bytesToBeRead);
			dataPacket = new DataPacket(requestedData, bytesToBeRead);
		}
		sessionNr = dataPacket.getSessionNr();
		packetNr = dataPacket.getPacketNr();
		data = dataPacket.returnSendData();
		System.out.println("SERVER: DATAPACKET RECEIVE " + request.getAddress().getHostAddress() + ": SESSIONR: "
				+ dataPacket.getSessionNr() + " PACKET: " + dataPacket.getPacketNr() + "\n");
	}
}
