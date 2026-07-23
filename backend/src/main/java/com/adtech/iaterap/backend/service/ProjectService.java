package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.entity.Project;
import com.adtech.iaterap.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

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

    public Project getProjectById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Project not found: " + id
                ));
    }

    public Project update(Long id, Project project) {
        Project existing = getProjectById(id);

        existing.setName(project.getName());
        existing.setDescription(project.getDescription());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
