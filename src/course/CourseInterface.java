package course;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
public interface CourseInterface {
    @WebMethod boolean validateStudent(String clientId);
    @WebMethod boolean validateAdvisor(String clientId);
    @WebMethod boolean addCourse(String id, String courseId, String courseName, String term, int seatsAvailable);
    @WebMethod void showCourses();
    @WebMethod boolean deleteCourse(String id, String courseId, String term, String department);
    @WebMethod String enrollCourse(String id, String term, String department, String courseId, boolean udpCall, boolean swapOperation, boolean checkCrossEnrollLimit);
    @WebMethod String displayCourses(String term);
    @WebMethod boolean dropCourse(String studentId, String courseId, String term, String department, boolean udpCall);
    @WebMethod String getClassSchedule(String studentId);
    @WebMethod String listCourseAvailability(String id, String term, String dept, boolean udpCall);
    @WebMethod String swapCourse(String id, String oldCourseId, String newCourseId, String term, String department);
}
