import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;


public class Server extends Thread{

	public static int DataLength;
     public static String sourceIP; // IP address of the client
     public static final int chunksize = 2048; // Should be the same packet size as the ones the client is sending, or greater
    public static byte[] DATA; // Total file cannot be more than 2 GB
    public static int seq; //  Packet sequence number
    public static byte[] dataReceived;
  

    public static String byteToString(byte Bytes){
        return String.valueOf( Integer.parseInt(String.format("%02X", Bytes), 16));
    }
    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value };
    }

    public Server(){
    }
    /**
     * This method receives the initial client handshake and sends an ACK in response
     * @throws IOException
     */
   public void waitForHandshake() throws IOException {
        DatagramSocket socketSend = new DatagramSocket();
        DatagramSocket socketReceive = new DatagramSocket(5555);
        byte[] ACK = {1};
        byte[] hs_ack = new byte[14];
        System.out.println("Awaiting handshake");
        DatagramPacket reply = new DatagramPacket(hs_ack, hs_ack.length);
        socketReceive.receive(reply);
        System.out.println("HS packet received");

        // Get the ip of the client, and file length
        if (hs_ack[0] != 0){
            seq = 0;
            byte[] dataLength = new byte[4];
            dataLength[0] = hs_ack[10];
            dataLength[1] = hs_ack[11];
            dataLength[2] = hs_ack[12];
            dataLength[3] = hs_ack[13];
            DataLength = ByteBuffer.wrap(dataLength).getInt();
            
            socketSend.send(new DatagramPacket(ACK, ACK.length, InetAddress.getByName(byteToString(hs_ack[2]) + "." + byteToString(hs_ack[3]) + "." +
                    byteToString(hs_ack[4]) + "." + byteToString(hs_ack[5])), 5555));
            sourceIP= byteToString(hs_ack[2]) + "." + byteToString(hs_ack[3]) + "." +
                    byteToString(hs_ack[4]) + "." + byteToString(hs_ack[5]);
            System.out.println("Ack is back");
        }
        socketReceive.close();
        socketSend.close();
    }

    /**
     * This method receives client data packets and consolidates the packets into suitable format
     * @throws IOException
     */
    public void DataIO() throws IOException {
        DATA = new byte[DataLength + chunksize];
        // at the start FIN flag is set 0 ie end of connection 
        int FIN = 0;
        
        DatagramSocket socketSend = new DatagramSocket();
        DatagramSocket socketReceive = new DatagramSocket(4444);

        // Socket has a set timeout in case a packet is lost
        socketReceive.setSoTimeout(10000);

        // While loop till FIN flag remains 0
        while (FIN != 1){
            dataReceived = new byte[chunksize + 5];
        
            
            try {
                socketReceive.receive(new DatagramPacket(dataReceived, dataReceived.length));
            } catch (SocketTimeoutException e){
                System.out.println("Timeout");
                byte[] ACK = intToByteArray(seq + 1);
                socketSend.send(new DatagramPacket(ACK, ACK.length, InetAddress.getByName(sourceIP), 4444));
                System.out.println("Ack sent");
                continue;
            }
            FIN = dataReceived[0];
            
            
            byte[] sequenceNumber = new byte[4];
            System.arraycopy(dataReceived, 1, sequenceNumber, 0, 4);
            seq= ByteBuffer.wrap(sequenceNumber).getInt();
            System.out.println("packet#: " + seq);

            // Copy the data from the received packet to the byte array in the appropriate location
            System.arraycopy(dataReceived, 5, DATA, seq * chunksize, chunksize);

            // Send an ACK with next sequence info
            byte[] ACK = intToByteArray(seq + 1);

            socketSend.send(new DatagramPacket(ACK, ACK.length, InetAddress.getByName(sourceIP), 4444));
            if(FIN == 1)
            	System.out.println("Transfer Complete");
            
        }
        socketReceive.close();
        socketSend.close();
        FileOutputStream fos = new FileOutputStream("output.jpg");
        fos.write(DATA);
        fos.close();
        
    }

    
    @Override
    public void run(){
        Server server = new Server();
        try {
            server.waitForHandshake();
            server.DataIO();
            System.out.println("done");
            } catch (IOException e) {
           
            }

    }
}