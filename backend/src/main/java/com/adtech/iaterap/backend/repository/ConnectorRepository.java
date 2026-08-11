package com.adtech.iaterap.backend.repository;
import com.adtech.iaterap.backend.entity.Connector;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ConnectorRepository extends JpaRepository<Connector, Long> {
    List<Connector> findByLruId(Long lruId);
}
