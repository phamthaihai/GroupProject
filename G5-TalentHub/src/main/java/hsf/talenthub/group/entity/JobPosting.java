package hsf.talenthub.group.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "job_postings")
public class JobPosting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 200)
    @NotNull
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 100)
    @NotNull
    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Size(max = 100)
    @NotNull
    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @NotNull
    @Column(name = "description", nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "requirements", length = Integer.MAX_VALUE)
    private String requirements;

    @Size(max = 100)
    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @ColumnDefault("'DRAFT'")
    @Column(name = "status", columnDefinition = "job_status not null")
    private Object status;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


}