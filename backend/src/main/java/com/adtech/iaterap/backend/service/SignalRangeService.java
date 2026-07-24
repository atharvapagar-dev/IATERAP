package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.SignalRange;
import com.adtech.iaterap.backend.repository.SignalRangeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SignalRangeService {

    private final SignalRangeRepository repository;

    public SignalRangeService(SignalRangeRepository repository) {
        this.repository = repository;
    }

    public List<SignalRange> getAll() {
        return repository.findAll();
    }

    public List<SignalRange> getBySignalLibraryId(Long signalLibraryId) {
        return repository.findBySignalLibraryId(signalLibraryId);
    }

    public SignalRange getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Signal range not found: " + id
                ));
    }

    public SignalRange save(SignalRange signalRange) {
        return repository.save(signalRange);
    }

    public SignalRange update(Long id, SignalRange signalRange) {
        SignalRange existing = getById(id);

        existing.setName(signalRange.getName());
        existing.setMinimumValue(signalRange.getMinimumValue());
        existing.setMaximumValue(signalRange.getMaximumValue());
        existing.setSeverity(signalRange.getSeverity());
        existing.setSignalLibrary(signalRange.getSignalLibrary());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
