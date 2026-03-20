package com.backtester.infrastructure.persistence.mapper;

import com.backtester.domain.market.Bar;
import com.backtester.infrastructure.persistence.entity.BarEntity;
import org.springframework.stereotype.Component;

@Component
public class BarEntityMapper {

    public BarEntity toEntity(Bar bar) {
        return new BarEntity(
                bar.ticker(),
                bar.date(),
                bar.open(),
                bar.high(),
                bar.low(),
                bar.close(),
                bar.volume()
        );
    }

    public Bar toDomain(BarEntity entity) {
        return new Bar(
                entity.getTicker(),
                entity.getDate(),
                entity.getOpen(),
                entity.getHigh(),
                entity.getLow(),
                entity.getClose(),
                entity.getVolume()
        );
    }
}
