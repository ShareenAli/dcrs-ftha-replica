package course;

import schema.UdpBody;
import schema.UdpPacket;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;

public class UdpOperations implements Serializable, Runnable {
    private int operation;
    private DatagramPacket packet;
    private DatagramSocket socket;
    private CourseOperations courseOperations;
    private Thread thread;
    private HashMap<String, Integer> courses = new HashMap<>();


    public UdpOperations(DatagramSocket socket, DatagramPacket packet, CourseOperations courseOperations) {
        this.socket = socket;
        this.packet = packet;
        this.courseOperations = courseOperations;
    }

    public void run() {
        String message;
        byte[] outgoingMessage;
        try {

            UdpPacket udpPacket = (UdpPacket) deserialize(this.packet.getData());
            UdpBody udpBody = udpPacket.getBody();
            System.out.println("UDP Packet Details : " + "\n" + udpBody.getCourseID() + "\n" + udpBody.getTerm() + "\n" + udpBody.getDepartment() + "\n" + udpBody.getStudentID());
            System.out.println("Operation : " + udpPacket.getOperation());
            switch (udpPacket.getOperation()) {
                case 1:
                    message = courseOperations.udpEnrollCourse(udpBody.getStudentID(), udpBody.getTerm(), udpBody.getDepartment(), udpBody.getCourseID());
                    System.out.println("Response from udpenroll" + message);
                    outgoingMessage = serialize(message);
                    break;
                case 2:
                    message = courseOperations.udpDropCourse(udpBody.getStudentID(), udpBody.getCourseID(), udpBody.getTerm());
                    System.out.println("Response from udpDropcourse" + message);
                    outgoingMessage = serialize(message);
                    break;
                case 3:
                    courses = courseOperations.udpListCourseAvailability(udpBody.getTerm());
                    System.out.println("Response from udp list course " + courses);
                    outgoingMessage = serialize(courses);
                    break;
                case 4:
                    message = courseOperations.deleteCourseStudentList(udpBody.getCourseID());
                    outgoingMessage = serialize(message);
                    break;
                default:
                    outgoingMessage = serialize("Error");
                    break;
            }
            DatagramPacket response = new DatagramPacket(outgoingMessage, outgoingMessage.length,
                    this.packet.getAddress(), packet.getPort());

            socket.send(response);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return o.readObject();
            }
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "New Udp Thread");
            thread.start();
        }
    }
}
