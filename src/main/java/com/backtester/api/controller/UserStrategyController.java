package com.backtester.api.controller;

import com.backtester.api.dto.request.CreateUserStrategyRequest;
import com.backtester.api.dto.response.UserStrategiesResponse;
import com.backtester.api.dto.response.UserStrategyDto;
import com.backtester.api.exception.ResourceNotFoundException;
import com.backtester.api.mapper.UserStrategyDtoMapper;
import com.backtester.application.strategy.UserStrategyService;
import com.backtester.domain.strategy.UserStrategyDefinition;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing user-defined strategy templates.
 *
 * <p>Base path: {@code /api/v1/user-strategies}
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /}      — Create a new template (returns 201 Created).</li>
 *   <li>{@code GET /}       — List all templates.</li>
 *   <li>{@code GET /{id}}   — Get a single template by UUID.</li>
 *   <li>{@code DELETE /{id}} — Delete a template (returns 204 No Content).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/user-strategies")
public class UserStrategyController {

    private final UserStrategyService service;
    private final UserStrategyDtoMapper mapper;

    /**
     * @param service Application service for managing strategy templates.
     * @param mapper  Mapper from domain records to API DTOs.
     */
    public UserStrategyController(UserStrategyService service, UserStrategyDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * Creates a new strategy template.
     *
     * @param request Template configuration.
     * @return The created template with its assigned UUID.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserStrategyDto create(@Valid @RequestBody CreateUserStrategyRequest request) {
        UserStrategyDefinition created = service.create(
                request.name(), request.baseStrategyId(), request.parameters());
        return mapper.toDto(created);
    }

    /**
     * Returns all saved strategy templates.
     *
     * @return List of templates with count.
     */
    @GetMapping
    public UserStrategiesResponse list() {
        List<UserStrategyDto> dtos = service.findAll().stream().map(mapper::toDto).toList();
        return new UserStrategiesResponse(dtos, dtos.size());
    }

    /**
     * Returns a single strategy template by its UUID.
     *
     * @param id Template UUID.
     * @return The template DTO.
     * @throws ResourceNotFoundException if not found.
     */
    @GetMapping("/{id}")
    public UserStrategyDto get(@PathVariable UUID id) {
        return service.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy template not found: " + id));
    }

    /**
     * Deletes a strategy template by its UUID.
     *
     * @param id Template UUID to delete.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.deleteById(id);
    }
}
