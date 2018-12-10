package schema;

import java.io.Serializable;
import java.util.ArrayList;

public class CourseData implements Serializable {

    private String course_id;
    private String course_name;
    private String term;
    private int course_capacity;
    private int seats_available;
    private ArrayList<String> enrolledStudents = new ArrayList<>();

    public CourseData(String course_id, String course_name, String term, int course_capacity) {
        this.course_id = course_id;
        this.course_name = course_name;
        this.term = term;
        this.course_capacity = course_capacity;
        this.enrolledStudents.clear();
    }

    public String getCourse_id() {
        return course_id;
    }

    public String getCourse_name() {
        return course_name;
    }

    public String getTerm() {
        return term;
    }

    public int getCourse_capacity() {
        return course_capacity;
    }

    public int getSeats_available() {
        seats_available = course_capacity - enrolledStudents.size();
        return seats_available;
    }

    public ArrayList<String> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(String enrolledStudents) {
        this.enrolledStudents.add(enrolledStudents);
    }

    public void setEnrolledStudents(ArrayList<String> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public boolean courseAvailability() {
        return this.enrolledStudents.size() >= this.course_capacity;
    }


}
