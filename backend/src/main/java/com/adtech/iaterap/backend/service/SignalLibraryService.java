package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.SignalLibrary;
import com.adtech.iaterap.backend.repository.SignalLibraryRepository;
import com.adtech.iaterap.backend.repository.SignalRangeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SignalLibraryService {

    private final SignalLibraryRepository repository;
    private final SignalRangeRepository signalRangeRepository;

    public SignalLibraryService(
            SignalLibraryRepository repository,
            SignalRangeRepository signalRangeRepository
    ) {
        this.repository = repository;
        this.signalRangeRepository = signalRangeRepository;
    }

    public List<SignalLibrary> getAll() {
        return repository.findAll();
    }

    public List<SignalLibrary> getByLruId(Long lruId) {
        return repository.findByLruId(lruId);
    }

    public List<SignalLibrary> getByInterfaceId(Long interfaceId) {
        return repository.findByInterfaceEntityId(interfaceId);
    }

    public SignalLibrary getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Signal library not found: " + id
                ));
    }

    public SignalLibrary save(SignalLibrary signalLibrary) {
        return repository.save(signalLibrary);
    }

    public SignalLibrary update(Long id, SignalLibrary signalLibrary) {
        SignalLibrary existing = getById(id);

        existing.setName(signalLibrary.getName());
        existing.setDescription(signalLibrary.getDescription());
        existing.setEngineeringUnit(signalLibrary.getEngineeringUnit());
        existing.setInterfaceEntity(signalLibrary.getInterfaceEntity());
        existing.setLru(signalLibrary.getLru());

        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        signalRangeRepository.deleteBySignalLibraryId(id);
        repository.deleteById(id);
    }
}
