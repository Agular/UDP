
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
	static long bytesRead;
	static int bufferSize = 1024;
	static long fileSize;
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
			System.out.println("SERVER: SENDING ACK: SESSIONNR: " + sessionNr + " PACKETNR: " + packetNr);
			socket.send(ackDatagrampacket);
		} catch (IOException e) {
			System.out.println("Could not send ACK!");
			e.printStackTrace();
		}

		outFile = new File("testOut.txt");
		try {
			outFile.createNewFile();
			outPutStream = new FileOutputStream(outFile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
				System.out.println(data.length);
				outPutStream.write(data);
				if(fileSize == bytesRead){
					outPutStream.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ack = new Ack(sessionNr, packetNr);
			ackDatagrampacket = new DatagramPacket(ack.returnData(), 3, inPacket.getAddress(), inPacket.getPort());
			try {
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
				+ startPacket.sessionNr + " PACKET: " + startPacket.packetNr);
		System.out.println("SERVER: Receiving file: " + startPacket.getFileName() + " size: "
				+ startPacket.getFileSize() + " bytes");
		System.out.println("SERVER: Included CRC32 checksum is: " + startPacket.getCrc() + "\n");
	}

	public static void readDataPacket(DatagramPacket request) throws Exception {
		// Obtain references to the packet's array of bytes.
		byte[] requestedData = request.getData();
		int bytesToBeRead;
		DataPacket dataPacket;
		// All conditions are assumed with the received packet as the last one.

		// If the packet is not the last one.
		if (bytesRead + bufferSize - 7 < fileSize) {
			System.out.println("NORMAL DATA PACKET");
			bytesToBeRead = bufferSize - 3;
			dataPacket = new DataPacket(requestedData, bytesToBeRead);
			bytesRead += bytesToBeRead;
			// The packet is last one and entirely full.
		} else if (bytesRead + bufferSize - 7 == fileSize) {
			System.out.println("LAST DATA PACKET (FULL)");
			bytesToBeRead = bufferSize - 7;
			dataPacket = new DataPacket(requestedData, bytesToBeRead);
			bytesRead += bytesToBeRead;
			// The packet is last but not full
		} else {
			System.out.println("LAST DATA PACKET (UNFULL)");
			bytesToBeRead = (int)(fileSize - bytesRead);
			System.out.println(bytesToBeRead);
			dataPacket = new DataPacket(requestedData, bytesToBeRead);
			bytesRead += bytesToBeRead;
		}
		sessionNr = dataPacket.getSessionNr();
		packetNr = dataPacket.getPacketNr();
		data = dataPacket.returnSendData();
		System.out.println("THE SIZE OF WRITABLE DATA: "+data.length);
		System.out.println("SERVER: DATAPACKET RECEIVE " + request.getAddress().getHostAddress() + ": SESSIONR: "
				+ dataPacket.getPacketNr() + " PACKET: " + dataPacket.getPacketNr());
	}
}