package schema;

import java.io.Serializable;

public class UdpBody implements Serializable {
    private String studentID;
    private String term;
    private String courseID;
    private String department;

    public UdpBody(String studentID, String term, String courseID, String department) {
        this.studentID = studentID;
        this.term = term;
        this.courseID = courseID;
        this.department = department;
    }

    public String getStudentID() {
        return studentID;
    }

    public String getTerm() {
        return term;
    }

    public String getCourseID() {
        return courseID;
    }

    public String getDepartment() {
        return department;
    }

}
