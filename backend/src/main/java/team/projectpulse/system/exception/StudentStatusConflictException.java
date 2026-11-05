package team.projectpulse.system.exception;

public class StudentStatusConflictException extends RuntimeException {
    public StudentStatusConflictException() {
        super("The student's account status changed. Reload the profile before saving again.");
    }
}
