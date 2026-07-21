package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Project;
import com.adtech.iaterap.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public List<Project> getAllProjects() {
        return repository.findAll();
    }

    public Project save(Project project) {
        return repository.save(project);
    }
}
