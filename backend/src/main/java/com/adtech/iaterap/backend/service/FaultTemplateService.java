package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.FaultTemplate;
import com.adtech.iaterap.backend.repository.FaultTemplateRepository;
import com.adtech.iaterap.backend.repository.FmeaBranchMappingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FaultTemplateService {

    private final FaultTemplateRepository repository;
    private final FmeaBranchMappingRepository fmeaBranchMappingRepository;

    public FaultTemplateService(
            FaultTemplateRepository repository,
            FmeaBranchMappingRepository fmeaBranchMappingRepository
    ) {
        this.repository = repository;
        this.fmeaBranchMappingRepository = fmeaBranchMappingRepository;
    }

    public List<FaultTemplate> getAll() {
        return repository.findAll();
    }

    public List<FaultTemplate> getByLruId(Long lruId) {
        return repository.findByLruId(lruId);
    }

    public FaultTemplate getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Fault template not found: " + id
                ));
    }

    public FaultTemplate save(FaultTemplate faultTemplate) {
        return repository.save(faultTemplate);
    }

    public FaultTemplate update(Long id, FaultTemplate faultTemplate) {
        FaultTemplate existing = getById(id);

        existing.setName(faultTemplate.getName());
        existing.setFaultCode(faultTemplate.getFaultCode());
        existing.setDescription(faultTemplate.getDescription());
        existing.setSeverity(faultTemplate.getSeverity());
        existing.setLru(faultTemplate.getLru());

        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        fmeaBranchMappingRepository.deleteByFaultTemplateId(id);
        repository.deleteById(id);
    }
}
