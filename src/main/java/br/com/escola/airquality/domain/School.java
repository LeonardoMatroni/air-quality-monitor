package br.com.escola.airquality.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "schools")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(unique = true, length = 18)
    private String cnpj;
    private String address;
    private String city;
    @Column(length = 2)
    private String state;
    @Column(name = "zip_code")
    private String zipCode;
    private String email;
    private String phone;
    @Builder.Default
    private boolean active = true;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Room> rooms;
    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
