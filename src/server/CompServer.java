package server;

import course.CourseOperations;
import course.UdpOperations;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class CompServer {
    public static void main(String[] args) {
        Logger logs = Logger.getLogger("comp server");

        try {
            FileHandler handler = new FileHandler("comp_server.log", true);
            logs.addHandler(handler);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        CourseOperations courseOperations = new CourseOperations();
        courseOperations.initializeValues("COMP", logs);

        UDPServerThread UDPServerThread = new UDPServerThread(courseOperations, 8004, "COMP", logs);
        UDPServerThread.start();

        try {
            DatagramSocket socket = new DatagramSocket(8001);

            byte[] buffer = new byte[1000];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                System.out.println("UDP COMP server running on: " + (8001));
                socket.receive(request);

                UdpOperations udpOp = new UdpOperations(socket, request, courseOperations);
                udpOp.start();
            }

        } catch (Exception e) {
            System.out.println("Exception:" + e);
        }
    }
}
