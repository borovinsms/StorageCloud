package ru.netology.storagecloud.repositories.tokens.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.storagecloud.repositories.tokens.entities.dao.TokenEntity;

public interface TokenJpaRepository extends JpaRepository<TokenEntity, String> {
}
