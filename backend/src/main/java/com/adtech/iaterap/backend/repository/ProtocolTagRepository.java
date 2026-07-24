package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.ProtocolTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProtocolTagRepository extends JpaRepository<ProtocolTag, Long> {

    List<ProtocolTag> findByLruId(Long lruId);
}
