package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.Lru;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LruRepository extends JpaRepository<Lru, Long> {
}