package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.FmeaBranchMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FmeaBranchMappingRepository extends JpaRepository<FmeaBranchMapping, Long> {

    List<FmeaBranchMapping> findByLruId(Long lruId);

    List<FmeaBranchMapping> findByFaultTemplateId(Long faultTemplateId);

    void deleteByFaultTemplateId(Long faultTemplateId);
}
