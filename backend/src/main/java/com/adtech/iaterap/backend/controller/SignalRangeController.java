package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.entity.SignalRange;
import com.adtech.iaterap.backend.service.SignalRangeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signal-ranges")
public class SignalRangeController {

    private final SignalRangeService service;

    public SignalRangeController(SignalRangeService service) {
        this.service = service;
    }

    @GetMapping
    public List<SignalRange> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SignalRange getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/by-signal-library/{signalLibraryId}")
    public List<SignalRange> getBySignalLibraryId(
            @PathVariable Long signalLibraryId
    ) {
        return service.getBySignalLibraryId(signalLibraryId);
    }

    @PostMapping
    public SignalRange create(@Valid @RequestBody SignalRange signalRange) {
        return service.save(signalRange);
    }

    @PutMapping("/{id}")
    public SignalRange update(
            @PathVariable Long id,
            @Valid @RequestBody SignalRange signalRange
    ) {
        return service.update(id, signalRange);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
