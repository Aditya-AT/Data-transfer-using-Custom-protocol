/**
* @author Aditya Ajit Tirakannavar
* FCN CSCI651
* Project # 3
* This main class instantiates starts a sender and receiver thread to send and receive data.
*/


import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length>0) {
        new Thread(new Client(Files.readAllBytes(Paths.get(args[1])), args[0])).start();
    }
        else{
        	new Thread(new Server()).start();
        }
    }
}