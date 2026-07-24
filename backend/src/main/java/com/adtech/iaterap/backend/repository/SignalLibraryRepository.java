package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.SignalLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalLibraryRepository extends JpaRepository<SignalLibrary, Long> {

    List<SignalLibrary> findByLruId(Long lruId);

    List<SignalLibrary> findByInterfaceEntityId(Long interfaceId);
}
