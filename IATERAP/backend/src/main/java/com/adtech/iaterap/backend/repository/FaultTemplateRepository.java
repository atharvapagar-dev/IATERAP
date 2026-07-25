package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.FaultTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaultTemplateRepository extends JpaRepository<FaultTemplate, Long> {

    List<FaultTemplate> findByLruId(Long lruId);
}
