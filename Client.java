import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;


public class Client extends Thread{

     static int chuncksize = 2048; // The size of each packet
     static String destAddr; // IP address to send data to
     static byte[] fileData; // Data that will be sent
     static int seq; // Packet sequence number


    public Client( byte[] fileData, String destAddr) {
        Client.fileData = fileData;
        Client.destAddr = destAddr;
    }


    /**
     * This method initializes a two-way handshake using UDP protocol. It creates a packet that contains a SYN flag which is initially set to 1,
     * and a sequence number 0 which is set by default. It also contains source, destination and data length as a part of handshake packet
     * Then, it waits for an ACK from the server side confirming the use of that sequence number
     * @throws IOException
     */
    
    
    public static byte stringToByte(String s){
        return (byte) Integer.parseInt(s);
    }
    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value };
    }
    public void init() throws IOException {
        DatagramSocket socketSend = new DatagramSocket();
        DatagramSocket socketReceive = new DatagramSocket(5555);
        byte[] receive = new byte[14];
        DatagramPacket reply = new DatagramPacket(receive, receive.length);

        byte[] header = new byte[14];
        header[0] = 1; // Indicates that it is a handshake
        header[1] = 0; // seq no

        // Get localhost/source IP address and adding it to packet
        String[] thisIP = InetAddress.getLocalHost().toString().strip().split("/");
        String localHost = thisIP[1];
        String[] localIP = localHost.strip().split("\\.");
        for (int i = 0; i < 4 ; i++) {
        	header[i+2] = stringToByte(localIP[i]);
        }

        // destination address
        String[] destIP = destAddr.strip().split("\\.");
        for (int i = 0; i < 4 ; i++) {
        	header[i+6] = stringToByte(destIP[i]);
        }

        // length of the file
        byte[] dataLength = intToByteArray(fileData.length);
        for (int i = 0; i < 4 ; i++) {
        	header[i+10] = Byte.parseByte(Integer.toString(dataLength[i]));
        }


        // Start handshake
        DatagramPacket handshakePacket = new DatagramPacket(header, header.length, InetAddress.getByName(destAddr), 5555);
        socketSend.send(handshakePacket);
        System.out.println("Handshake started");

        // Wait for a response from the server
        socketReceive.receive(reply);
        if (receive[0] == 1){
        	//checking if SYN bit is set and setting seq num value to 0 
            seq = 0;
            System.out.println("Connection established!");
        }
        socketReceive.close();
        socketSend.close();
    }

    /**
     * Reads input file, then sends the data to the server
     * Byte 0: FIN flag
     * Bytes 1-4: Packet sequence number
     * Bytes: 5-end: Data
     * @throws IOException
     */
    public void DataIO() throws IOException {
        DatagramSocket socketSend = new DatagramSocket();
        DatagramSocket socketReceive = new DatagramSocket(4444);
        socketReceive.setSoTimeout(1000);

        // While loop sends all packets from starting sequence number, to the second last packet.
        // Last packet will generally be of a different length
        while (seq <= ((fileData.length / chuncksize)-1)){
            byte[] sendpacket = new byte[chuncksize + 5];
            sendpacket[0] = 0;
            System.arraycopy(intToByteArray(seq), 0, sendpacket, 1, 4);
            System.arraycopy(fileData, seq * chuncksize, sendpacket, 5, chuncksize);
            socketSend.send(new DatagramPacket(sendpacket, sendpacket.length, InetAddress.getByName(destAddr), 4444));
            System.out.println("Sending packet# " + seq);


            // Wait for an ACK from the server
            byte[] ACK = new byte[4];
            DatagramPacket ACKFromServer = new DatagramPacket(ACK, ACK.length);

            try {
                socketReceive.receive(ACKFromServer);
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout");
                continue;
            }

            // New sequence number is the one contained in the ACK packet that the server sends
            seq = ByteBuffer.wrap(ACK).getInt();
        }

        // This block takes care of sending the last packet, as the size would be smaller
        byte[] sendlastpkt = new byte[5 + fileData.length - (seq * chuncksize)];

        // Set the FIN flag to 1, indicating this is the last packet, and the connection should end
        sendlastpkt[0] = 1;
        System.arraycopy(intToByteArray(seq), 0, sendlastpkt, 1, 4);
        System.arraycopy(fileData, seq * chuncksize, sendlastpkt, 5, (fileData.length - (seq * chuncksize)));
        socketSend.send(new DatagramPacket(sendlastpkt, sendlastpkt.length, InetAddress.getByName(destAddr), 4444));
        System.out.println("Sending packet# " + seq);
        System.out.println("All packets sent");
        socketReceive.close();
        socketSend.close();

    }
    @Override
    public void run() {
        Client c = new Client(fileData, destAddr);
        try {
            c.init();
            c.DataIO();
        } catch (IOException e) {
        }

    }
}