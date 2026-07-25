package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.Connector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectorRepository extends JpaRepository<Connector, Long> {
}