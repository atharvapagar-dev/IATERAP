package com.adtech.iaterap.backend.repository;
import com.adtech.iaterap.backend.entity.Interface;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InterfaceRepository extends JpaRepository<Interface, Long> {
    List<Interface> findByLruId(Long lruId);
}
