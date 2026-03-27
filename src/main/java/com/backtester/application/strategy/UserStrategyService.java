package com.backtester.application.strategy;

import com.backtester.application.port.UserStrategyDefinitionRepository;
import com.backtester.domain.strategy.UserStrategyDefinition;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for managing user-defined strategy templates.
 * Templates store a name, base strategy type, and parameter overrides
 * that are applied at backtest execution time via {@code Strategy.withParameters()}.
 */
@Service
public class UserStrategyService {

    private final UserStrategyDefinitionRepository repository;

    /**
     * @param repository Port for persisting and querying user strategy definitions.
     */
    public UserStrategyService(UserStrategyDefinitionRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and persists a new strategy template.
     *
     * @param name           Human-readable label for this template.
     * @param baseStrategyId The registered strategy type to use (e.g. "MOMENTUM_V1").
     * @param parameters     Parameter overrides (e.g. {@code {"lookbackDays": 30}});
     *                       may be null or empty.
     * @return The saved template with a generated UUID and creation timestamp.
     */
    public UserStrategyDefinition create(String name, String baseStrategyId, Map<String, Object> parameters) {
        UserStrategyDefinition definition = new UserStrategyDefinition(
                UUID.randomUUID(), name, baseStrategyId,
                parameters == null ? Map.of() : parameters,
                Instant.now()
        );
        return repository.save(definition);
    }

    /**
     * Fetches a template by ID.
     *
     * @param id Template UUID.
     * @return The template, or empty if not found.
     */
    public Optional<UserStrategyDefinition> findById(UUID id) {
        return repository.findById(id);
    }

    /**
     * Returns all saved templates.
     *
     * @return List of all templates in insertion order.
     */
    public List<UserStrategyDefinition> findAll() {
        return repository.findAll();
    }

    /**
     * Deletes a template by ID.
     *
     * @param id Template UUID to delete.
     */
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
