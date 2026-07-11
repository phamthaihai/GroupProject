package com.example.groupproject.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads/cvs").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a file");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must not exceed 5 MB");
        }

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String lowercaseFilename = originalFilename.toLowerCase();
        if (!lowercaseFilename.endsWith(".pdf") && !lowercaseFilename.endsWith(".docx")) {
            throw new IllegalArgumentException("Only PDF or DOCX files are allowed");
        }

        String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "uploads/cvs/" + uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + originalFilename + ". Please try again!", e);
        }
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}
