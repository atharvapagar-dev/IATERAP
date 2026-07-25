package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.FmeaBranchMapping;
import com.adtech.iaterap.backend.repository.FmeaBranchMappingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FmeaBranchMappingService {

    private final FmeaBranchMappingRepository repository;

    public FmeaBranchMappingService(FmeaBranchMappingRepository repository) {
        this.repository = repository;
    }

    public List<FmeaBranchMapping> getAll() {
        return repository.findAll();
    }

    public List<FmeaBranchMapping> getByLruId(Long lruId) {
        return repository.findByLruId(lruId);
    }

    public List<FmeaBranchMapping> getByFaultTemplateId(Long faultTemplateId) {
        return repository.findByFaultTemplateId(faultTemplateId);
    }

    public FmeaBranchMapping getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "FMEA branch mapping not found: " + id
                ));
    }

    public FmeaBranchMapping save(FmeaBranchMapping fmeaBranchMapping) {
        return repository.save(fmeaBranchMapping);
    }

    public FmeaBranchMapping update(Long id, FmeaBranchMapping fmeaBranchMapping) {
        FmeaBranchMapping existing = getById(id);

        existing.setFmeaBranchCode(fmeaBranchMapping.getFmeaBranchCode());
        existing.setFailureEffect(fmeaBranchMapping.getFailureEffect());
        existing.setFailureCause(fmeaBranchMapping.getFailureCause());
        existing.setDetectionMethod(fmeaBranchMapping.getDetectionMethod());
        existing.setRecommendedAction(fmeaBranchMapping.getRecommendedAction());
        existing.setRiskPriorityNumber(fmeaBranchMapping.getRiskPriorityNumber());
        existing.setLru(fmeaBranchMapping.getLru());
        existing.setFaultTemplate(fmeaBranchMapping.getFaultTemplate());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
