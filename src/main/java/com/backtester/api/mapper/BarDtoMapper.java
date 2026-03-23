package com.backtester.api.mapper;

import com.backtester.api.dto.request.IngestBarsRequest;
import com.backtester.api.dto.response.BarDto;
import com.backtester.domain.market.Bar;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts between {@link Bar} domain records and their API DTO equivalents.
 */
@Component
public class BarDtoMapper {

    /**
     * Converts a list of API bar data records to domain {@link Bar} objects.
     * The ticker is forced to uppercase on each created bar.
     *
     * @param ticker      Uppercase ticker symbol to assign to each bar.
     * @param barDataList API request bar data list.
     * @return Domain bar list.
     */
    public List<Bar> toDomain(String ticker, List<IngestBarsRequest.BarData> barDataList) {
        return barDataList.stream()
                .map(bd -> new Bar(ticker.toUpperCase(), bd.date(), bd.open(), bd.high(), bd.low(), bd.close(), bd.volume()))
                .toList();
    }

    /**
     * Converts a domain {@link Bar} to a {@link BarDto} for API responses.
     *
     * @param bar Domain bar record.
     * @return API response DTO.
     */
    public BarDto toDto(Bar bar) {
        return new BarDto(bar.ticker(), bar.date(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume());
    }
}
