package server;

import course.CourseOperations;
import course.UdpOperations;
import schema.HoldBack;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;

public class UDPServerThread implements Runnable {
    private HoldBack holdBack = HoldBack.getInstance();

    private String ipAddress = "230.1.1.4";
    private int fePort = 7779;
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
            DatagramSocket socket = new DatagramSocket(port);

            byte[] buffer = new byte[1000];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                logs.info("UDP " + serverName + " server running on : " + port);
                System.out.println("UDP " + serverName + " server running on : " + port);
                socket.receive(request);

                String content = (String) deserialize(request.getData());
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

        String result = this.courseOperations.selectOperation(operationName, contents);
        byte[] outgoing = serialize(result);
        DatagramPacket reply = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName(ipAddress), fePort);
        socket.send(reply);

        holdBack.removeFromQueue();

        holdBack.incrementLastSequence();
        this.processQueue(socket);
    }

    public void start() {
        Thread thread = new Thread(this, "New Ws Thread");
        thread.start();
    }

    private static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    private static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return o.readObject();
            }
        }
    }
}
