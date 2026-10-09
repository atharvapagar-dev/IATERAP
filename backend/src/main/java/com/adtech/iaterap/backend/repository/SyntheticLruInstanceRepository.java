package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.SyntheticLruInstance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyntheticLruInstanceRepository extends JpaRepository<SyntheticLruInstance, Long> {
    List<SyntheticLruInstance> findByLruIdAndLifecycleStatus(Long lruId, String lifecycleStatus);
    Optional<SyntheticLruInstance> findByInstanceCode(String instanceCode);
}
