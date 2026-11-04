package com.ptu.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_items",
        indexes = {
                @Index(name = "idx_inventory_owner", columnList = "owner_student_id"),
                @Index(name = "idx_inventory_case", columnList = "case_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRef;

    @Column(name = "owner_student_id", nullable = false)
    private UUID ownerStudentId;

    @Column(name = "item_name", nullable = false, length = 120)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseRarity rarity;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @PrePersist
    public void onCreate() {
        this.acquiredAt = LocalDateTime.now();
    }
}
