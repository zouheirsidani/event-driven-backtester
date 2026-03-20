package com.backtester.api.dto.response;

import java.util.List;

public record SymbolsResponse(List<SymbolDto> symbols, int count) {}
