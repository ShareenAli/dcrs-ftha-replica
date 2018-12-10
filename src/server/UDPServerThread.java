package server;

import course.CourseOperations;
import course.UdpOperations;
import schema.HoldBack;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.Logger;

public class UDPServerThread implements Runnable {
    private HoldBack holdBack = HoldBack.getInstance();

    private String ipAddress = "230.1.1.4";
    private int rmPort = 5001;
    private CourseOperations courseOperations;
    private int port;
    private String serverName;
    private Logger logs;

    UDPServerThread(CourseOperations operations, int port, String serverName, Logger logs) {
        this.courseOperations = operations;
        this.port = port;
        this.serverName = serverName;
        this.logs = logs;
    }

    @Override
    public void run() {
        try {
        	MulticastSocket socket = new MulticastSocket(port);
//          DatagramSocket socket = new DatagramSocket(8033);
        	socket.joinGroup(InetAddress.getByName("230.1.1.1"));
      	
            logs.info("Sequencer " + serverName + " server listening on : " + port);

            byte[] buffer = new byte[1000];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String content = new String(request.getData());
                String contents[] = content.split("-");

                holdBack.addToQueue(Integer.parseInt(contents[0]), content);

                processQueue(socket);
            }

        } catch (Exception e) {
            System.out.println("Exception:" + e);
        }
    }

    private void processQueue(DatagramSocket socket) throws IOException {
        if (!holdBack.isThereIsNext())
            return;

        String content = holdBack.getNextRequest();
        String contents[] = content.split("-");
        String operationName = contents[1];
        
        System.out.println(content);

        String result = this.courseOperations.selectOperation(operationName, contents);
        byte[] outgoing = result.getBytes();
        DatagramPacket reply = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName(ipAddress), Integer.parseInt(contents[contents.length - 1].trim()));
        DatagramPacket rmReply = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName("localhost"), rmPort);
        socket.send(reply);
        socket.send(rmReply);
        
        holdBack.removeFromQueue();

        holdBack.incrementLastSequence();
        this.processQueue(socket);
    }

    public void start() {
        Thread thread = new Thread(this, "New Ws Thread");
        thread.start();
    }
}
