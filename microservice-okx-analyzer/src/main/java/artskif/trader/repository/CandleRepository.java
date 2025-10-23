package artskif.trader.repository;

import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CandleRepository implements PanacheRepositoryBase<Candle, CandleId> { }
