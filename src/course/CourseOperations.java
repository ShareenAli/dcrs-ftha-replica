
package course;

import schema.CourseData;
import schema.UdpBody;
import schema.UdpPacket;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.org.apache.xml.internal.security.keys.keyresolver.implementations.SecretKeyResolver;

public class CourseOperations {
    private Logger logs;
    private HashMap<String, HashMap<String, CourseData>> courseDetails = new HashMap<>();
    private HashMap<String, HashMap<String, List<String>>> studentTermWiseDetails = new HashMap<>();
    private String advisorID;
    private int CompPort = 8001, SoenPort = 8002, InsePort = 8003;

    public void initializeValues(String deptName, Logger logs) {
        this.logs = logs;
        advisorID = deptName.toUpperCase() + "A1001";
        for (int i = 1; i < 6; i++) {
            HashMap<String, List<String>> studentCourseDetails = new HashMap<>();
            String studentID = deptName + "S100".concat(String.valueOf(i));
            studentTermWiseDetails.put(studentID, studentCourseDetails);
        }
    }

    public String selectOperation(String operationName, String contents[]) {
        String result = "";
        String studentDepartmentName, courseDepartmentName, advisorDepartmentName;
        boolean udpCall;
        switch (operationName.toLowerCase()){
            case "addcourse" :
            	System.out.println(contents[6]);
                result = addCourse(contents[2], contents[3], contents[5], contents[5],contents[4], Integer.parseInt(contents[6].replaceAll("[^0-9]", "")));
                break;
            case "removecourse" :
                advisorDepartmentName = contents[3].substring(0,4);
                result = deleteCourse(contents[2], contents[3], contents[5].trim(), contents[4], advisorDepartmentName);
                break;
            case "enrolcourse" :
                studentDepartmentName = contents[3].substring(0,4);
                courseDepartmentName = contents[5].substring(0,4);

                udpCall = !studentDepartmentName.equalsIgnoreCase(courseDepartmentName);
                result  = enrollCourse(contents[2], contents[3], contents[4], studentDepartmentName, String.valueOf(contents[5].trim()), udpCall, false, true);
                break;
            case  "dropcourse" :
                studentDepartmentName = contents[3].substring(0,4);
                courseDepartmentName = contents[5].substring(0,4);

                udpCall = !studentDepartmentName.equalsIgnoreCase(courseDepartmentName);
                result = dropCourse(contents[2], contents[3], contents[5].trim(), contents[4], studentDepartmentName, udpCall);
                break;
            case "classsche" :
                result = getClassSchedule(contents[2], contents[3].trim());
                break;
            case "courseavl" :
                advisorDepartmentName= contents[3].substring(0, 4);
                result = listCourseAvailability(contents[2], contents[3], contents[4].trim(), advisorDepartmentName);
                break;
            case "swapcourse" :
                studentDepartmentName = contents[3].substring(0,4);
                result = swapCourse(contents[2], contents[3], contents[5], contents[6].trim(), contents[4], studentDepartmentName);
                break;
        }

        return result;
    }

    public String validateStudent(String clientId) {
        for (Map.Entry<String, HashMap<String, List<String>>> studentIDlist : this.studentTermWiseDetails.entrySet()) {
            String studentID = studentIDlist.getKey();
            if (studentID.equalsIgnoreCase(clientId)) {
                return "true";
            }
        }
        return "false";
    }

    public String validateAdvisor(String clientId) {
        if (advisorID.equalsIgnoreCase(clientId)){
            return "true";
        }
        return "false";
    }

    public String addCourse(String serverName, String advisorID, String courseID, String courseName, String term, int courseCapacity) {
    	if(!serverName.equalsIgnoreCase(courseID.substring(0,4))) {
    		System.out.println(advisorID + " can add courses only of pattern " + serverName.toUpperCase() + "####.");
    		return "replica1-" + serverName + "-no course added";
    	} 
        if (courseDetails.containsKey(term)) {
            HashMap<String, CourseData> termMap = courseDetails.get(term);
            if (termMap.containsKey(courseID)) {
                logs.warning("This course ID already exists for this term. Please try again!");
                System.out.println("This course ID already exists for this term. Please try again!");
                return "replica1-" + serverName + "-no course added";
            } else {
                CourseData newCourseData = new CourseData(courseID, courseName, term, courseCapacity);
                termMap.put(courseID, newCourseData);
                courseDetails.put(term, termMap);
                showCourses();
                logs.info(advisorID + " added course " + courseID + " successfully!");
                return "replica1-" + serverName + "-course added";
            }
        } else {
            HashMap<String, CourseData> courseMap = new HashMap<>();
            CourseData courseData = new CourseData(courseID, courseName, term, courseCapacity);
            courseMap.put(courseData.getCourse_id(), courseData);
            courseDetails.put(courseData.getTerm(), courseMap);
            showCourses();
            System.out.println(courseData.getTerm() + " term. " + courseData.getCourse_id());
            logs.info("Term added to HashMap and course added Successfully by " + advisorID);
            return "replica1-" + serverName + "-course added";
        }
    }

    public void showCourses() {
        System.out.println("\n New Operation : \n");
        for (Map.Entry<String, HashMap<String, CourseData>> term : this.courseDetails.entrySet()) {
            String termName = term.getKey();
            for (Map.Entry<String, CourseData> course : term.getValue().entrySet()) {
                String courseId = course.getKey();
                CourseData courseDataDetails = course.getValue();
                logs.info("Term : " + termName + "\n Course ID: " + courseId +
                        "\n Course Name : " + courseDataDetails.getCourse_name() +
                        "\n Total Capacity : " + courseDataDetails.getCourse_capacity() +
                        "\n Seats Available : " + courseDataDetails.getSeats_available() +
                        "\n Course Added Successfully!");
                System.out.println("Term : " + termName + "\n Course ID: " + courseId +
                        "\n Course Name : " + courseDataDetails.getCourse_name() +
                        "\n Total Capacity : " + courseDataDetails.getCourse_capacity() +
                        "\n Seats Available : " + courseDataDetails.getSeats_available() +
                        "\n Course Added Successfully!");
            }
        }
    }

    public String deleteCourse(String serverName, String advisorID, String courseId, String term, String department) {
        int[] ports = new int[2];
        if (courseDetails.containsKey(term)) {
            HashMap<String, CourseData> termMap = this.courseDetails.get(term);
            if (termMap.containsKey(courseId)) {
                termMap.remove(courseId);
                courseDetails.put(term, termMap);
                System.out.println(term + " " + termMap.get(courseId));

                 for (Map.Entry<String, HashMap<String, List<String>>> studentTermCourseDetails : this.studentTermWiseDetails.entrySet()) {
                    String studentId = studentTermCourseDetails.getKey();
                    HashMap<String, List<String>> termCourses = studentTermCourseDetails.getValue();
                    for (Map.Entry<String, List<String>> termCoursesList : termCourses.entrySet()) {
                        String leTerm = termCoursesList.getKey();
                        List<String> courseIDlist = termCoursesList.getValue();
                        courseIDlist.remove(courseId);
                        termCourses.put(leTerm, courseIDlist);
                    }
                    this.studentTermWiseDetails.put(studentId, termCourses);
                }
                deleteCourseStudentList(courseId);

                if (courseId.substring(0, 4).equalsIgnoreCase("COMP")) {
                    ports[0] = SoenPort;
                    ports[1] = InsePort;
                } else if (courseId.substring(0, 4).equalsIgnoreCase("SOEN")) {
                    ports[0] = CompPort;
                    ports[1] = InsePort;
                } else {
                    ports[0] = CompPort;
                    ports[1] = SoenPort;
                }

                UdpBody udpBody = new UdpBody(courseId, term, courseId, department);
                UdpPacket udpPacket = new UdpPacket(4, udpBody);
                System.out.println("OPERATION1: " + udpPacket.getOperation());
                String responseObj = (String) udpPacketInfo(udpPacket, ports[0]);
                System.out.println("RESPONSE1: " + responseObj);

                UdpBody udpBody2 = new UdpBody(courseId, term, courseId, department);
                UdpPacket udpPacket2 = new UdpPacket(4, udpBody2);
                System.out.println("OPERATION2: " + udpPacket.getOperation());
                String responseObj2 = (String) udpPacketInfo(udpPacket2, ports[1]);
                System.out.println("RESPONSE2: " + responseObj2);
                if (responseObj == "true" && responseObj2 == "true"){
                    logs.info(advisorID + " has deleted " + courseId + "course successfully!");
                    return "replica1-" + serverName + "-course removed";
                } else {
                    return "replica1-" + serverName + "-no course removed";
                }
            } else {
                logs.warning("There is no course with " + courseId + " CourseData ID.");
                System.out.println("There is no course with " + courseId + " CourseData ID.");
                return "replica1-" + serverName + "-no course removed";
            }
        } else {
            logs.warning("Please enter valid term name. Try Again!");
            System.out.println("Please enter valid term name. Try Again!");
        }
        return "replica1-" + serverName + "-no course removed";
    }

    synchronized public String enrollCourse(String serverName, String id, String term, String department, String courseId, boolean udpCall, boolean swapOperation, boolean checkCrossEnrollLimit) {
        HashMap<String, CourseData> termMap = courseDetails.get(term);
        HashMap<String, List<String>> studentCourseDetails = studentTermWiseDetails.get(id);
        int port, enrollLimit = 0;

        if (udpCall) {
            logs.info("UDP details : " + "\n" + id + "\n" + term + "\n" + department + "\n" + courseId);

            if (studentCourseDetails.containsKey(term)) {
                List<String> courses = studentCourseDetails.get(term);
                if (courses != null) {

                    for (String course : courses) {
                        System.out.println("COURSEIDS: " + course);
                        System.out.println("department : " + department);
                        System.out.println("Student ID : " + id.substring(0, 4));
                        if (!(course.substring(0, 4).equalsIgnoreCase(department))) {
                            enrollLimit++;
                        }
                    }
                    if (enrollLimit == 2 && checkCrossEnrollLimit) {
                        return "replica1-" + serverName + "-dept limit is full";
                    }
                    if (courses.size() == 3 && !swapOperation) {
                        return "replica1-" + serverName + "-capacity for this term is full";
                    }
                    if (courses.contains(courseId)) {
                        return "replica1-" + serverName + "-no success";
                    }
                }
            }
            if (courseId.substring(0, 4).equalsIgnoreCase("COMP")) {
                port = CompPort;
            } else if (courseId.substring(0, 4).equalsIgnoreCase("SOEN")) {
                port = SoenPort;
            } else {
                port = InsePort;
            }
            System.out.println("Port : " + port);
            UdpBody udpBody = new UdpBody(id, term, courseId, department);
            UdpPacket udpPacket = new UdpPacket(1, udpBody);
            System.out.println("Check in enroll method");
            System.out.println(udpPacket.getOperation());
            String responseObj = (String) udpPacketInfo(udpPacket, port);
            if (responseObj.equalsIgnoreCase("enrolledSuccessfully")) {
                if (studentCourseDetails.containsKey(term)) {
                    List<String> courses = studentCourseDetails.get(term);
                    courses.add(courseId);
                    studentCourseDetails.put(term, courses);
                    studentTermWiseDetails.put(id, studentCourseDetails);
                } else {
                    List<String> coursesList = new ArrayList<>();
                    coursesList.add(courseId);
                    studentCourseDetails.put(term, coursesList);
                    studentTermWiseDetails.put(id, studentCourseDetails);
                }
                displayStudentDetails();
                return "replica1-" + serverName + "-enroll success";
            } else {
                return responseObj;
            }
        } else {
            if (courseDetails.containsKey(term)) {
                CourseData course = termMap.get(courseId.toString());
                if (course != null) {
                    List<String> courses = studentCourseDetails.get(term);
                    if (course.courseAvailability()) {
                    	System.out.println("replica1-" + serverName + "-course capacity is full");
                        return "replica1-" + serverName + "-course capacity is full";
                    }
                    if (studentCourseDetails.containsKey(term)) {
                        if (courses.size() == 3 && !swapOperation) {
                        	System.out.println("replica1-" + serverName + "-capacity for this term is full");
                        	return "replica1-" + serverName + "-capacity for this term is full";
                        }
                        if (courses.contains(courseId)) {
                        	System.out.println("replica1-" + serverName + "-no success");
                        	return "replica1-" + serverName + "-no success";
                        }
                    }
                    course.setEnrolledStudents(id);
                    termMap.put(courseId, course);
                    courseDetails.put(term, termMap);
                    if (studentCourseDetails.containsKey(term)) {
                        courses.add(courseId);
                        studentCourseDetails.put(term, courses);
                        studentTermWiseDetails.put(id, studentCourseDetails);
                    } else {
                        List<String> coursesList = new ArrayList<>();
                        coursesList.add(courseId);
                        studentCourseDetails.put(term, coursesList);
                        studentTermWiseDetails.put(id, studentCourseDetails);
                    }
                    displayStudentDetails();
                    System.out.println("replica1-" + serverName + "-enroll success");
                    return "replica1-" + serverName + "-enroll success";
                } else {
                	System.out.println("replica1-" + serverName + "-there is no such course in the database");
                    return "replica1-" + serverName + "-there is no such course in the database";
                }
            }
            return "replica1-" + serverName + "-no success";
        }
    }

    public String displayCourses(String term) {
        String message = "";
        HashMap<String, CourseData> termCourses = this.courseDetails.get(term);
        for (Map.Entry<String, CourseData> theTerm : termCourses.entrySet()) {
            String courseID = theTerm.getKey();
            message = message.concat("\n -> Course ID : " + courseID);
        }
        return message;
    }

    public String dropCourse(String serverName, String studentId, String courseId, String term, String department, boolean udpCall) {
        int port;
        if (udpCall) {
            System.out.println("dropCourse = " + courseId);
            if (courseId.length() <= 8) {
                if (courseId.substring(0, 4).equalsIgnoreCase("COMP")) {
                    port = CompPort;
                } else if (courseId.substring(0, 4).equalsIgnoreCase("SOEN")) {
                    port = SoenPort;
                } else {
                    port = InsePort;
                }
                System.out.println("Port : " + port);
                UdpBody udpBody = new UdpBody(studentId, term, courseId, department);
                UdpPacket udpPacket = new UdpPacket(2, udpBody);
                System.out.println("Check in enroll method");
                System.out.println(udpPacket.getOperation());
                String responseObj = (String) udpPacketInfo(udpPacket, port);

                if (responseObj.equalsIgnoreCase("true")) {
                    HashMap<String, List<String>> studentTermCourseDetails = this.studentTermWiseDetails.get(studentId);
                    List<String> course = studentTermCourseDetails.get(term);
                    boolean result = course.remove(courseId);
                    if (result) {
                        studentTermCourseDetails.put(term, course);
                        studentTermWiseDetails.put(studentId, studentTermCourseDetails);
                        System.out.println(courseId + " course has been dropped by " + studentId + " student for " + term + " term.");
                        displayStudentDetails();
                        return "replica1-" + serverName + "-drop success";
                    } else {
                        System.out.println("Couldn't find the course");
                        return "replica1-" + serverName + "-there is no such course in the database";
                    }
                } else {
                    return "replica1-" + serverName + "-there is no such course in the database";
                }
            } else {
                System.out.println("Course can only be of the pattern COMP####/SOEN####/INSE####.");
            }

        } else {
            if (courseDetails.containsKey(term)) {
                HashMap<String, CourseData> termMap = this.courseDetails.get(term);
                if (termMap.containsKey(courseId)) {
                    CourseData courseDataDetails = termMap.get(courseId);
                    ArrayList<String> studentIDlist = courseDataDetails.getEnrolledStudents();
                    studentIDlist.remove(studentId);
                    courseDataDetails.setEnrolledStudents(studentIDlist);
                    HashMap<String, List<String>> studentTermCourseDetails = this.studentTermWiseDetails.get(studentId);
                    List<String> course = studentTermCourseDetails.get(term);
                    boolean result = course.remove(courseId);
                    if (result) {
                        studentTermCourseDetails.put(term, course);
                        studentTermWiseDetails.put(studentId, studentTermCourseDetails);
                        System.out.println(courseId + " course has been dropped by " + studentId + " student for " + term + " term.");
                        displayStudentDetails();
                        return "replica1-" + serverName + "-drop success";
                    } else {
                        System.out.println("Couldn't find the course");
                        return "replica1-" + serverName + "-there is no such course in the database";
                    }
                } else {
                    System.out.println("There is no course with " + courseId + " CourseData ID.");

                }
            } else {
                System.out.println("Please enter valid term name. Try Again!");
            }
        }
        return "replica1-" + serverName + "-there is no such course in the database";
    }

    public String getClassSchedule(String serverName, String studentId) {
        String message = "replica1-" + serverName + "-Schedule ";
        HashMap<String, List<String>> studentTermCourseDetails = this.studentTermWiseDetails.get(studentId);
        for (Map.Entry<String, List<String>> studentClassSchedule : studentTermCourseDetails.entrySet()) {
            List<String> courseList = studentClassSchedule.getValue();
            for (String course : courseList) {
                message = message.concat(" " + course + " " + studentClassSchedule.getKey());
            }
        }
        return message;
    }

    @SuppressWarnings("unchecked")
    public String listCourseAvailability(String serverName, String id, String term, String department) {
        HashMap<String, Integer> listCourse = new HashMap<>();
        System.out.println(term);
        HashMap<String, CourseData> termMap = courseDetails.get(term);
        listCourse.clear();
        int[] ports = new int[2];

        if (id.substring(0, 4).equalsIgnoreCase("COMP")) {
            ports[0] = SoenPort;
            ports[1] = InsePort;
        } else if (id.substring(0, 4).equalsIgnoreCase("SOEN")) {
            ports[0] = CompPort;
            ports[1] = InsePort;
        } else {
            ports[0] = CompPort;
            ports[1] = SoenPort;
        }

        System.out.println("PORTS 1:" + ports[0]);
        System.out.println("PORTS 2:" + ports[1]);

        UdpBody udpBody = new UdpBody(id, term, "", department);
        UdpPacket udpPacket = new UdpPacket(3, udpBody);
        System.out.println("Check in list course method");
//        System.out.println(udpPacket.getOperation());
        HashMap<String, Integer> responseObj = (HashMap<String, Integer>) udpPacketInfo(udpPacket, ports[0]);
        System.out.println("LIST1: " + responseObj);

        UdpBody udpBody2 = new UdpBody(id, term, "", department);
        UdpPacket udpPacket2 = new UdpPacket(3, udpBody2);
        System.out.println("Check in list course method 2");
//        System.out.println(udpPacket.getOperation());
        HashMap<String, Integer> responseObj2 = (HashMap<String, Integer>) udpPacketInfo(udpPacket2, ports[1]);
        System.out.println("LIST2: " + responseObj2);

        listCourse.putAll(responseObj);
        listCourse.putAll(responseObj2);

        if (termMap != null) {
            for (Map.Entry<String, CourseData> courseEntry : termMap.entrySet()) {
                CourseData courseData = courseEntry.getValue();
                int space = courseData.getCourse_capacity() - courseData.getEnrolledStudents().size();
                System.out.println("COURSEID: " + courseData.getCourse_id() + "SPACE: " + space);
                listCourse.put(courseData.getCourse_id(), space);
            }
            return "replica1-" + serverName + "-" + this.listCourseAvailabilityString(listCourse, term);
        }

        System.out.println(listCourse.size());

        return "replica1-" + serverName + "-" + this.listCourseAvailabilityString(listCourse, term);
    }

    public String swapCourse(String serverName, String id, String oldCourseId, String newCourseId, String term, String department) {
        System.out.println("Swapping course");
        String newCourseIdDept = newCourseId.substring(0, 4);
        String oldCourseIdDept = oldCourseId.substring(0, 4);
        String studentIdDept = id.substring(0, 4);
        boolean udpCall = false, crossEnrollLimitCheck = false;
        String result;

        if (!(newCourseIdDept.equalsIgnoreCase(department)) || !(studentIdDept.equalsIgnoreCase(newCourseIdDept))) {
            System.out.println("udp is true");
            udpCall = true;
        }

        if (oldCourseIdDept.equalsIgnoreCase(department) && (!newCourseIdDept.equalsIgnoreCase(department)))
            crossEnrollLimitCheck = true;

        if (udpCall) {
            result = enrollCourse(serverName, id, term, department, newCourseId, true, true, crossEnrollLimitCheck);
            System.out.println("RESULT FROM ENROLL(SWAP): " + result);

            if (result.equalsIgnoreCase("replica1-" + serverName + "-enroll success")) {
                boolean udp = !oldCourseId.toUpperCase().startsWith(department.toUpperCase());
                String dropCourseResult = dropCourse(serverName, id, oldCourseId, term, department,udp);
                if (dropCourseResult.equalsIgnoreCase("replica1-" + serverName + "-drop success"))
                    return "replica1-" + serverName + "-the course swap is success";
                else {
                    String dropAgain = dropCourse(serverName, id, newCourseId, term, department, true);
                    System.out.println("REVERT UDP CALL CHANGES: " + dropAgain);
                    return "replica1-" + serverName + "-course swap is unsuccess";
                }
            } else if (result.equalsIgnoreCase("replica1-" + serverName + "-dept limit is full"))
                return "replica1-" + serverName + "-course swap is unsuccess";
            else if (result.equalsIgnoreCase("replica1-" + serverName + "-no success"))
                return "replica1-" + serverName + "-course swap is unsuccess";
            else if (result.equalsIgnoreCase("replica1-" + serverName + "-course capacity is full"))
                return "replica1-" + serverName + "-course swap is unsuccess";
            else if (result.equalsIgnoreCase("replica1-" + serverName + "-there is no such course in the database"))
                return "replica1-" + serverName + "-course swap is unsuccess";
        } else {
            result = enrollCourse(serverName, id, term, department, newCourseId, false, true, crossEnrollLimitCheck);
            if (result.equalsIgnoreCase("replica1-" + serverName + "-enroll success")) {
                System.out.println("udp = " + oldCourseId.toUpperCase().startsWith(department.toUpperCase()));
                boolean udp = !oldCourseId.toUpperCase().startsWith(department.toUpperCase());
                String dropResult = dropCourse(serverName, id, oldCourseId, term, department, udp);
                if (dropResult.equalsIgnoreCase("replica1-" + serverName + "-drop success"))
                    return "replica1-" + serverName + "-the course swap is success";
                else {
                    String dropAgain = dropCourse(serverName, id, newCourseId, term, department, false);
                    System.out.println("REVERT CHANGES: " + dropAgain);
                    return "replica1-" + serverName + "-course swap is unsuccess";
                }
            } else if (result.equalsIgnoreCase("replica1-" + serverName + "-dept limit is full"))
                return "replica1-" + serverName + "-course swap is unsuccess";
            else if (result.equalsIgnoreCase("replica1-" + serverName + "-no success"))
                return "replica1-" + serverName + "-course swap is unsuccess";
            else if (result.equalsIgnoreCase("replica1-" + serverName + "-course capacity is full"))
                return "replica1-" + serverName + "-course swap is unsuccess";
            else if (result.equalsIgnoreCase("replica1-" + serverName + "-there is no such course in the database"))
                return "replica1-" + serverName + "-course swap is unsuccess";
        }
        return "replica1-" + serverName + "-course swap is unsuccess";
    }

    synchronized String udpEnrollCourse(String id, String term, String department, String course_id) {
        HashMap<String, CourseData> termMap = courseDetails.get(term);
        if (courseDetails.containsKey(term)) {
            CourseData courseData = termMap.get(course_id);
            if (courseData != null) {
                if (courseData.courseAvailability()) {
                    return "courseFull";
                }
                System.out.println("UDP details from udpenroll: " + "\n" + id + "\n" + term + "\n" + department + "\n" + course_id);
                courseData.setEnrolledStudents(id);
                termMap.put(course_id, courseData);
                courseDetails.put(term, termMap);
                return "enrolledSuccessfully";
            } else {
                return "courseNotFound";
            }
        } else {
            return "courseNotFound";
        }
    }

    synchronized String udpDropCourse(String studentID, String courseID, String term) {
        if (courseDetails.containsKey(term)) {
            HashMap<String, CourseData> termMap = this.courseDetails.get(term);
            if (termMap.containsKey(courseID)) {
                CourseData courseDataDetails = termMap.get(courseID);
                ArrayList<String> studentIDlist = courseDataDetails.getEnrolledStudents();
                studentIDlist.remove(studentID);
                courseDataDetails.setEnrolledStudents(studentIDlist);
                return "true";
            }
        }
        return "false";
    }

    HashMap<String, Integer> udpListCourseAvailability(String term) {
        HashMap<String, Integer> listCourse = new HashMap<>();
        HashMap<String, CourseData> termMap = courseDetails.get(term);
        if (termMap != null) {
            for (Map.Entry<String, CourseData> courseEntry : termMap.entrySet()) {
                CourseData courseData = courseEntry.getValue();
                int space = courseData.getCourse_capacity() - courseData.getEnrolledStudents().size();
                System.out.println("Course ID: " + courseData.getCourse_id() + "Course space: " + space);
                listCourse.put(courseData.getCourse_id(), space);
            }
            return listCourse;
        }
        return listCourse;
    }

    private String listCourseAvailabilityString(HashMap<String, Integer> courses, String term) {
        String message = "";
        if (courses.size() > 0) {
            for (Map.Entry<String, Integer> coursesList : courses.entrySet()) {
                String courseID = coursesList.getKey();
                Integer courseDetails = coursesList.getValue();
                message = message.concat(courseID + " " + courseDetails + " ");
            }
        } else
            message = message.concat(" ");
        return message;
    }

    private void displayStudentDetails() {
        System.out.println("New Operation : ");
        for (Map.Entry<String, HashMap<String, List<String>>> studentTermCourseDetails : this.studentTermWiseDetails.entrySet()) {
            String studentID = studentTermCourseDetails.getKey();
            System.out.println("StudentId: " + studentID);
            for (Map.Entry<String, List<String>> termCoursesList : studentTermCourseDetails.getValue().entrySet()) {
                List<String> courseList = termCoursesList.getValue();
                String term = termCoursesList.getKey();
                System.out.print(studentID + " is enrolled for ");
                for (String course : courseList) {
                    System.out.print(course + " ");
                }
                System.out.print(" in " + term + "\n");
            }
        }
    }

    String deleteCourseStudentList(String courseID) {
        for (Map.Entry<String, HashMap<String, List<String>>> studentTermCourseDetails : this.studentTermWiseDetails.entrySet()) {
            String studentId = studentTermCourseDetails.getKey();
            HashMap<String, List<String>> courseDetails = studentTermCourseDetails.getValue();
            for (Map.Entry<String, List<String>> termCoursesList : courseDetails.entrySet()) {
                String term = termCoursesList.getKey();
                List<String> coursesList = termCoursesList.getValue();
                coursesList.remove(courseID);
                courseDetails.put(term, coursesList);
            }
            this.studentTermWiseDetails.put(studentId, courseDetails);
        }
        return "Successfully deleted!";
    }

    private Object udpPacketInfo(UdpPacket udpPacket, int port) {
        try {
            Object response;
            DatagramSocket socket = new DatagramSocket();

            System.out.println("CHECK FROM UDPPACKET METHOD");
            byte[] requestMessage = serialize(udpPacket);
            DatagramPacket requestPacket = new DatagramPacket(requestMessage, requestMessage.length,
                    InetAddress.getByName("localhost"), port);
            socket.send(requestPacket);

            // incoming
            byte[] responseMessage = new byte[1000];
            DatagramPacket responsePacket = new DatagramPacket(responseMessage, responseMessage.length);
            socket.receive(responsePacket);

            response = deserialize(responsePacket.getData());
            return response;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "Error in server";
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
}
