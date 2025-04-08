package com.example.teletravailservice.repository;


import com.example.teletravailservice.entity.TeletravailRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeletravailRequestRepository extends JpaRepository<TeletravailRequest, Long> {
}