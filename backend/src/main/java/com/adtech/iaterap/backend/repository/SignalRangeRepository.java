package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.SignalRange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalRangeRepository extends JpaRepository<SignalRange, Long> {

    List<SignalRange> findBySignalLibraryId(Long signalLibraryId);

    void deleteBySignalLibraryId(Long signalLibraryId);
}
