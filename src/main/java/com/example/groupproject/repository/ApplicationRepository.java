package com.example.groupproject.repository;
import com.example.groupproject.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    // Tự động có các hàm: save(), findById(), findAll(), deleteById()...
}