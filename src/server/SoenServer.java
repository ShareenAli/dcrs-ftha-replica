package server;

import course.CourseOperations;
import course.UdpOperations;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class SoenServer {
    public static void main(String[] args) {
        Logger logs = Logger.getLogger("SOEN Server");

        try {
            FileHandler handler = new FileHandler("soen_server.log", true);
            logs.addHandler(handler);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        CourseOperations courseOperations = new CourseOperations();
        courseOperations.initializeValues("SOEN", logs);

        UDPServerThread UDPServerThread = new UDPServerThread(courseOperations, 8005, "SOEN", logs);
        UDPServerThread.start();

        try {
            DatagramSocket socket = new DatagramSocket(8002);

            byte[] buffer = new byte[1000];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                System.out.println("UDP SOEN server running on: " + (8002));
                socket.receive(request);

                UdpOperations udpOp = new UdpOperations(socket, request, courseOperations);
                udpOp.start();
            }

        } catch (Exception e) {
            System.out.println("Exception:" + e);
        }
    }
}
