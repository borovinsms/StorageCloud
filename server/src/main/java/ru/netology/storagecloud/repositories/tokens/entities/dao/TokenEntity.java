package ru.netology.storagecloud.repositories.tokens.entities.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "tokens")
public class TokenEntity {

    @Id
    private String username;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private long start;

    @Column(nullable = false)
    private long expiration;

    @Column(nullable = false, name = "is_active")
    private boolean isActive;
}
