package ru.netology.storagecloud.repositories.files.dao.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.netology.storagecloud.repositories.files.dao.entities.FileEntity;

import java.util.List;
import java.util.Optional;

public interface FileJpaRepository extends JpaRepository<FileEntity, Integer> {

    @Query(nativeQuery = true, value = "select * from files f where f.username = ?2 limit ?1")
    List<FileEntity> getAllWithLimit(int count, String username);

    Optional<FileEntity> findByFileNameAndUsername(String fileName, String username);
}
