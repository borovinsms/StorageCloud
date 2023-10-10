package ru.netology.storagecloud.repositories.tokens.entities.dao;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "authorities")
public class AuthorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 45, nullable = false)
    private String authority;

    @Column(length = 45, nullable = false)
    private String username;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AuthorityEntity authority = (AuthorityEntity) o;
        return Objects.equals(id, authority.id) && Objects.equals(this.authority, authority.authority);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
