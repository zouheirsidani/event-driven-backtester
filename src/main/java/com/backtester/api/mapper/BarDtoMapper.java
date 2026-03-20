package com.backtester.api.mapper;

import com.backtester.api.dto.request.IngestBarsRequest;
import com.backtester.api.dto.response.BarDto;
import com.backtester.domain.market.Bar;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BarDtoMapper {

    public List<Bar> toDomain(String ticker, List<IngestBarsRequest.BarData> barDataList) {
        return barDataList.stream()
                .map(bd -> new Bar(ticker.toUpperCase(), bd.date(), bd.open(), bd.high(), bd.low(), bd.close(), bd.volume()))
                .toList();
    }

    public BarDto toDto(Bar bar) {
        return new BarDto(bar.ticker(), bar.date(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume());
    }
}
