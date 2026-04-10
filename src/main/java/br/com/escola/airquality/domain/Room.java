package br.com.escola.airquality.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private School school;
    @Column(nullable = false, length = 100)
    private String name;
    private String floor;
    private Integer capacity;
    @Column(name = "area_m2", precision = 6, scale = 2)
    private BigDecimal areaM2;
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 50)
    @Builder.Default
    private RoomType roomType = RoomType.CLASSROOM;
    @Builder.Default
    private boolean active = true;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Sensor> sensors;
    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }
    public enum RoomType { CLASSROOM, LABORATORY, LIBRARY, CAFETERIA, GYM, OFFICE, CORRIDOR, BATHROOM }
}
