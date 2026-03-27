package com.backtester.application.port;

import com.backtester.domain.strategy.UserStrategyDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for user-defined strategy templates.
 * Implemented in the infrastructure layer; consumed by application services only.
 */
public interface UserStrategyDefinitionRepository {

    /**
     * Persists a new or updated definition.
     *
     * @param definition The definition to save.
     * @return The saved definition (may contain generated fields).
     */
    UserStrategyDefinition save(UserStrategyDefinition definition);

    /**
     * Returns a definition by its ID, or empty if not found.
     *
     * @param id The UUID to look up.
     * @return An {@link Optional} containing the definition, or empty if absent.
     */
    Optional<UserStrategyDefinition> findById(UUID id);

    /**
     * Returns all saved definitions in insertion order.
     *
     * @return List of all definitions; empty if none exist.
     */
    List<UserStrategyDefinition> findAll();

    /**
     * Deletes the definition with the given ID. No-op if not found.
     *
     * @param id The UUID of the definition to delete.
     */
    void deleteById(UUID id);
}
