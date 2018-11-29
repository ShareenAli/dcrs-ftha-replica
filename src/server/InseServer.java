package server;

import course.CourseOperations;
import course.UdpOperations;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class InseServer {
    public static void main(String[] args) {
        Logger logs = Logger.getLogger("Inse Server");

        try {
            FileHandler handler = new FileHandler("inse_server.log", true);
            logs.addHandler(handler);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        CourseOperations courseOperations = new CourseOperations();
        courseOperations.initializeValues("INSE", "Inse Server");

        WsThread wsThread = new WsThread(courseOperations, "http://localhost:8202/ws/inseserver");
        wsThread.start();

        try {
            DatagramSocket socket = new DatagramSocket(8003);

            byte[] buffer = new byte[1000];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                System.out.println("UDP server running on: " + (8003));
                socket.receive(request);

                UdpOperations udpOp = new UdpOperations(socket, request, courseOperations);
                udpOp.start();
            }

        } catch (Exception e) {
            System.out.println("Exception:" + e);
        }
    }
}
