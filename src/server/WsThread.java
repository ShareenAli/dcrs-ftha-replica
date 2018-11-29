package server;

import course.CourseOperations;

import javax.xml.ws.Endpoint;

public class WsThread implements Runnable {
    private CourseOperations courseOperations;
    private String endpointUrl;

    WsThread(CourseOperations operations, String url) {
        this.courseOperations = operations;
        this.endpointUrl = url;
    }

    @Override
    public void run() {
        Endpoint.publish(endpointUrl, courseOperations);
        System.out.println("Endpoint published at: " + endpointUrl);
    }

    public void start() {
        Thread thread = new Thread(this, "New Ws Thread");
        thread.start();
    }
}
