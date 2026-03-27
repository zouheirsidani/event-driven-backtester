package com.backtester.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code user_strategy_definitions} table.
 * The {@code parameters} column is stored as PostgreSQL JSONB using Hibernate's
 * native JSON type support.
 */
@Entity
@Table(name = "user_strategy_definitions")
public class UserStrategyDefinitionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_strategy_id", nullable = false)
    private String baseStrategyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> parameters;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Required no-arg constructor for JPA. */
    protected UserStrategyDefinitionEntity() {}

    /**
     * Full constructor for creating new entity instances.
     *
     * @param id             Unique identifier.
     * @param name           Human-readable display name.
     * @param baseStrategyId Base strategy type identifier.
     * @param parameters     Parameter override map stored as JSONB.
     * @param createdAt      Creation timestamp.
     */
    public UserStrategyDefinitionEntity(UUID id, String name, String baseStrategyId,
                                         Map<String, Object> parameters, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.baseStrategyId = baseStrategyId;
        this.parameters = parameters;
        this.createdAt = createdAt;
    }

    /** @return The unique identifier. */
    public UUID getId() { return id; }

    /** @return The human-readable display name. */
    public String getName() { return name; }

    /** @return The base strategy type identifier. */
    public String getBaseStrategyId() { return baseStrategyId; }

    /** @return The parameter override map. */
    public Map<String, Object> getParameters() { return parameters; }

    /** @return The creation timestamp. */
    public Instant getCreatedAt() { return createdAt; }
}
