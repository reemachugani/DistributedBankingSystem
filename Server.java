import java.net.*;
import java.util.*;
import java.io.*;


public class Server {

    static HashSet<Integer> set = new HashSet<Integer>();
    public static void main(String args[]) throws Exception
    {
        readAccountNumFile();

        DatagramSocket serverSocket = new DatagramSocket(2222);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        while(true)
        {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            String sentence = new String( receivePacket.getData());
            System.out.println("RECEIVED: " + sentence);
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            String capitalizedSentence = sentence.toUpperCase();
            sendData = capitalizedSentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
        }
    }

    public static void readAccountNumFile() throws IOException{
        File file = new File("AccNum");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        while((line = br.readLine())!=null){
            set.add(Integer.parseInt(line));
        }
    }

}
