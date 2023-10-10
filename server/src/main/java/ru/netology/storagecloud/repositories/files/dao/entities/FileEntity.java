package ru.netology.storagecloud.repositories.files.dao.entities;


import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "files", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_filename", columnList = "file_name")
})
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, name = "path_directory")
    private String pathDirectory;

    @Column(nullable = false, name = "file_name")
    private String fileName;

    @Column(nullable = false)
    private int size;

    @Column(nullable = false)
    private String username;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !obj.getClass().equals(this.getClass())) return false;
        FileEntity fileEntity = (FileEntity) obj;
        return this.id == fileEntity.id
                && Objects.equals(this.pathDirectory, fileEntity.pathDirectory)
                && Objects.equals(this.fileName, fileEntity.fileName)
                && this.size == fileEntity.size
                && Objects.equals(this.username, fileEntity.username);
    }
}
