
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.zip.CRC32;

public class Client {

	public static void main(String[] args) {
		/*
		 * Read input. Exit if input is not inserted correctly.
		 */
		if (args.length != 3) {
			System.out.println("Input: IP Port file_name.ext Exiting...");
			System.exit(-1);
		}
		/*
		 * Create instances of need arguments.
		 */
		String ipArg = args[0];
		int port = Integer.parseInt(args[1]);
		String fileName = args[2];
		Random rand = new Random();
		short sessionNr = (short) rand.nextInt(Short.MAX_VALUE + 1);
		DatagramSocket socket = null;
		InetAddress IP = null;
		InputStream inFileStream = null;
		File inFile;
		byte[] buf;
		int bufferSize = 1024;
		int packetNr = 0;
		long totalBytesRead;
		long fileSize;
		/*
		 * Open a socket. Exit if failed.
		 */
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println("SOCKET CREATION FAILED! Exiting...");
			e.printStackTrace();
			System.exit(-1);
		}

		/*
		 * Get target for socket. Exit if failed.
		 */
		try {
			IP = InetAddress.getByName(ipArg);
		} catch (UnknownHostException e) {
			System.out.println("Server could not be determined! Exiting...");
			e.printStackTrace();
			System.exit(-1);
		}
		/*
		 * Open the file to be sent. Exit if failed.
		 */
		inFile = new File(fileName);
		try {
			inFileStream = new FileInputStream(inFile);
			fileSize = inFile.length();
		} catch (FileNotFoundException e1) {
			System.out.println("File not found! Exiting...");
			e1.printStackTrace();
			System.exit(-1);
		}
		/*
		 * Create startPacket and send it.
		 */
		StartPacket startPacket = new StartPacket(sessionNr, packetNr, fileName, fileSize);
		byte[] data = startPacket.returnData();
		DatagramPacket sendPacket = new DatagramPacket(data, data.length, IP, port);
		DatagramPacket receiveAck = new DatagramPacket(new byte[3], 3);
		while (true) {
			try {
				System.out.println("CLIENT: SESSIONNR: " + sessionNr);
				System.out.println("Sending file " + fileName + " to " + IP.getHostName() + ":" + port + " SessionID: "
						+ sessionNr);
				System.out
						.println("Size: " + inFile.length() + " bytes. CRC32 checksum " + startPacket.getCrc() + "\n");
				socket.send(sendPacket);

			} catch (IOException e) {
				System.out.println("Error while sending a packet");
				e.printStackTrace();
			}
			try {
				socket.receive(receiveAck);
				break;
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		/////////SEND STARTPACKET 10 TIMES MAX AND TIMEOUT IF OVER 10!!!!!!!!!!!!!!!!!!!!
		Ack ack = new Ack(receiveAck.getData());
		System.out.println(
				"CLIENT: ACK FROM SERVER: SESSIONNR: " + ack.getSessionNr() + " PACKETNR: " + ack.getPacketNr());
		;
		/*
		 * Send file data.
		 */
		int bytesToBeRead;
		boolean lastPacket = false;
		//buf = new byte[buffersize-3];
		try {
			// There is more data to be read.
			if(totalBytesRead + bufferSize - 3 <fileSize){
				bytesToBeRead = bufferSize - 3;
				buf = new byte[bytesToBeRead];
			}
			// The last packet is filled perfectly.
			else if(totalBytesRead + bufferSize - 7 == fileSize){
				bytesToBeRead = bufferSize - 7;
				buf = new byte[bytesToBeRead];
				lastPacket = true;
			}
			// It is the last packet, but half-filled.
			else if (totalBytesRead + bufferSize - 7 > fileSize){
				bytesToBeRead = (int)(fileSize - totalBytesRead);
				buf = new byte[bytesToBeRead];
				lastPacket = true;
			} 
			// Reading all into buffer would result client not be able to write crc, so we are leaving the last byte out.
			else if ((totalBytesRead + bufferSize - 7 < fileSize) &&( fileSize - totalBytesRead) <= (long)(bufferSize - 3)) ){
				bytesToBeRead = (int)(fileSize - totalBytesRead -1);
				buf = new byte[bytesToBeRead];
			}
			while ((bytesRead = inFileStream.read(buf, 0, buffersize-3)) != -1) {
				packetNr += 1;
				DataPacket dataPacket = new DataPacket(sessionNr, packetNr, buf, bytesRead);
				data = dataPacket.returnData();
				sendPacket = new DatagramPacket(data, data.length, IP, port);
				socket.send(sendPacket);
			}
		} catch (IOException e1) {
			System.out.println("Error reading from file stream.");
			e1.printStackTrace();
		}

		// CLEANUP
		try {
			inFileStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		socket.close();
	}
}
