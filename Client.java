
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
		byte[] buf = null;
		int bufferSize = 1024;
		int packetNr = 0;
		long totalBytesRead = 0;
		long fileSize = 0;
		final int MAX_FAILED_CONNECTIONS = 10;
		int failed_connections = 0;
		int timeOutMs = 1000;
		long sendTime = 0;
		long receiveTime;
		long RTT;
		double datarate;
		long totalTime = 0;

		/*
		 * Open a socket. Exit if failed.
		 */
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(timeOutMs);
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
		Ack ack;

		// Will send Startpacket a number of MAX_FAILED_CONNECTIONS times.
		// Need to include server time out!
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
				ack = new Ack(receiveAck.getData());
				if (isAckCorrect(ack, sessionNr, packetNr)) {
					System.out.println("CLIENT: ACK FROM SERVER: SESSIONNR: " + ack.getSessionNr() + " PACKETNR: "
							+ ack.getPacketNr() + "\n");
					failed_connections = 0;
					break;
				} else {
					failed_connections += 1;
					if (failed_connections == MAX_FAILED_CONNECTIONS) {
						System.out.println("CLIENT: SERVER FAILED 10 TIMES TO SEND CORRECT ACK");
						System.out.println("CLIENT: EXITING");
						System.exit(-1);
					}
				}

			} catch (SocketTimeoutException to) {
				System.out.println("CLIENT: SERVER TIMED OUT!");
				to.printStackTrace();
				failed_connections += 1;
				if (failed_connections == MAX_FAILED_CONNECTIONS) {
					System.out.println("CLIENT: SERVER FAILED 10 TIMES TO SEND CORRECT ACK");
					System.out.println("CLIENT: EXITING");
					System.exit(-1);
				}
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		packetNr += 1;
		/*
		 * Send file data.
		 */
		int bytesToBeRead = 0;
		boolean lastPacket = false;
		boolean ackIsReceived = false;
		CRC32 crc32 = new CRC32();
		DataPacket dataPacket;
		while (fileSize != totalBytesRead) {
			try {
				// There is more data to be read.
				if (totalBytesRead + bufferSize - 3 < fileSize) {
					bytesToBeRead = bufferSize - 3;
					buf = new byte[bytesToBeRead];
				}
				// The last packet is filled perfectly.
				else if (totalBytesRead + bufferSize - 7 == fileSize) {
					bytesToBeRead = bufferSize - 7;
					buf = new byte[bytesToBeRead];
					lastPacket = true;
				}
				// It is the last packet, but half-filled.
				else if (totalBytesRead + bufferSize - 7 > fileSize) {
					bytesToBeRead = (int) (fileSize - totalBytesRead);
					buf = new byte[bytesToBeRead];
					lastPacket = true;
				}
				// Crc can not be fitted with the datapacket, so it would be
				// sent later separately.
				else if ((totalBytesRead + bufferSize - 7 < fileSize)
						&& ((fileSize - totalBytesRead) <= (long) (bufferSize - 3))) {
					bytesToBeRead = (int) (fileSize - totalBytesRead);
					buf = new byte[bytesToBeRead];
				}
				int bytesRead = inFileStream.read(buf, 0, bytesToBeRead);
				totalBytesRead += bytesRead;
				crc32.update(buf, 0, bytesToBeRead);
				if (!lastPacket) {
					dataPacket = new DataPacket(sessionNr, packetNr, buf, bytesRead);
					System.out.println("SENDING NORMAL DATAPACKET" + "SN: " + dataPacket.getSessionNr() + " PKN: "
							+ dataPacket.getPacketNr() + " DATASIZE: " + dataPacket.getDataSize() + "\n");
				} else {
					dataPacket = new DataPacket(sessionNr, packetNr, buf, bytesRead, crc32.getValue());
					System.out.println("SENDING LAST DATAPACKET WITH CRC");
					System.out.println("SESSIONNR: " + dataPacket.getSessionNr() + " PACKETNR: "
							+ dataPacket.getPacketNr() + " DATASIZE: " + dataPacket.getDataSize());
					System.out.println("CLIENT: CRC OF ALL DATA: " + dataPacket.getICrc() + "\n");
				}

				data = dataPacket.returnData();
				sendPacket = new DatagramPacket(data, data.length, IP, port);
				System.out.println();
				while (true) {
					if (sendTime == 0) {
						sendTime = System.nanoTime();
					}
					socket.send(sendPacket);
					try {
						socket.receive(receiveAck);
						RTT = System.nanoTime() - sendTime;
						totalTime += RTT;
						sendTime = 0;
						if (bytesRead != 0) {
							datarate = ((double) (bytesRead) * 8.0 / 1024.0) / ((double) (RTT) / (10 * 9));
						} else {
							datarate = (double) (4 * 8 / 1024) / (double) (RTT / (10 * 9));
						}

						datarate = (double) (Math.round(datarate * 1000) / 1000);
						ack = new Ack(receiveAck.getData());
						if (isAckCorrect(ack, sessionNr, packetNr)) {
							System.out.println("CLIENT: ACK FROM SERVER: SESSIONNR: " + ack.getSessionNr()
									+ " PACKETNR: " + ack.getPacketNr() + " RTT: " + datarate + " kBits/s" + "\n");
							failed_connections = 0;
							break;
						} else {
							failed_connections += 1;
							if (failed_connections == MAX_FAILED_CONNECTIONS) {
								System.out.println("CLIENT: SERVER FAILED 10 TIMES TO SEND CORRECT ACK");
								System.out.println("CLIENT: EXITING");
								System.exit(-1);
							}
						}

					} catch (SocketTimeoutException to) {
						System.out.println("CLIENT: SERVER TIMED OUT!");
						to.printStackTrace();
						failed_connections += 1;
						if (failed_connections == MAX_FAILED_CONNECTIONS) {
							System.out.println("CLIENT: SERVER FAILED 10 TIMES TO SEND CORRECT ACK");
							System.out.println("CLIENT: EXITING");
							System.exit(-1);
						}
					}
				}
			} catch (IOException e1) {
				System.out.println("CLIENT: Error reading from file stream.");
				e1.printStackTrace();
			}
			packetNr = (packetNr + 1) % 2;
		}

		if (!lastPacket) {
			try {
				System.out.println("CLIENT: SENDING LAST DATAPACKET ONLY CRC");
				dataPacket = new DataPacket(sessionNr, packetNr, crc32.getValue());
				data = dataPacket.returnData();
				System.out.println("CLIENT: CRC OF ALL DATA: " + dataPacket.getICrc());
				sendPacket = new DatagramPacket(data, data.length, IP, port);
				while (true) {
					if (sendTime == 0) {
						sendTime = System.nanoTime();
					}
					socket.send(sendPacket);
					try {
						socket.receive(receiveAck);
						RTT = System.nanoTime() - sendTime;
						totalTime += RTT;
						sendTime = 0;
						datarate = (double) (4 * 8 / 1024) / (double) (RTT / (10 * 9));
						datarate = (double) (Math.round(datarate * 1000) / 1000);
						ack = new Ack(receiveAck.getData());
						if (isAckCorrect(ack, sessionNr, packetNr)) {
							System.out.println("CLIENT: ACK FROM SERVER: SESSIONNR: " + ack.getSessionNr()
									+ " PACKETNR: " + ack.getPacketNr() + " RTT: " + datarate + " kBits/s" + "\n");
							failed_connections = 0;
							break;
						} else {
							failed_connections += 1;
							if (failed_connections == MAX_FAILED_CONNECTIONS) {
								System.out.println("CLIENT: SERVER FAILED 10 TIMES TO SEND CORRECT ACK");
								System.out.println("CLIENT: EXITING");
								System.exit(-1);
							}
						}

					} catch (SocketTimeoutException to) {
						System.out.println("CLIENT: SERVER TIMED OUT!");
						to.printStackTrace();
						failed_connections += 1;
						if (failed_connections == MAX_FAILED_CONNECTIONS) {
							System.out.println("CLIENT: SERVER FAILED 10 TIMES TO SEND CORRECT ACK");
							System.out.println("CLIENT: EXITING");
							System.exit(-1);
						}
					}
				}
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		// CLEANUP
		try {
			inFileStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		socket.close();

		// SOME STATISTICS
		double totalTimeS = (double) (totalTime) / (10 * 9);
		double avgDatarate = (double) (fileSize) * 8 / 1024 / totalTimeS;
		avgDatarate = (double) (Math.round(avgDatarate * 1000) / 1000);
		System.out.println("SERVER: TOTAL TIME FOR SENDING: " + totalTimeS + " seconds");
		System.out.println("SERVER: AVERAGE DATARATE: " + avgDatarate + " kBits/s");
	}

	public static boolean isAckCorrect(Ack ack, int currentSessionNr, int currentPacketNr) {
		if (ack.getSessionNr() != currentSessionNr || ack.getPacketNr() != currentPacketNr) {
			return false;
		}
		return true;
	}
}
