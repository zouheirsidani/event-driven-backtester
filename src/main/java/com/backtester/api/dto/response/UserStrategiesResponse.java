package com.backtester.api.dto.response;

import java.util.List;

/**
 * Response envelope for {@code GET /api/v1/user-strategies}.
 *
 * @param templates All saved user strategy templates.
 * @param count     Number of templates in this response.
 */
public record UserStrategiesResponse(List<UserStrategyDto> templates, int count) {}
