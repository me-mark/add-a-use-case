package team.projectpulse.student;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>, JpaSpecificationExecutor<Student> {

    String PROFILE_SCOPE = """
            from Student s left join s.section section left join section.course course
            where s.id = :studentId and (
                (:allowSelf = true and s.id = :callerId) or
                (:staff = true and (course.courseAdmin.id = :callerId or exists (
                    select i.id from Section assigned join assigned.instructors i
                    where assigned = section and i.id = :callerId
                )))
            )
            """;

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s " + PROFILE_SCOPE)
    Optional<Student> findProfileForUpdate(@Param("studentId") Integer studentId,
                                           @Param("callerId") Integer callerId,
                                           @Param("allowSelf") boolean allowSelf,
                                           @Param("staff") boolean staff);

    @Query("select count(s) " + PROFILE_SCOPE)
    long countAccessibleProfile(@Param("studentId") Integer studentId,
                                @Param("callerId") Integer callerId,
                                @Param("allowSelf") boolean allowSelf,
                                @Param("staff") boolean staff);

    // Find all students in a section
    List<Student> findBySectionSectionId(Integer sectionId);

    List<Student> findByTeamTeamId(Integer teamId);
}
