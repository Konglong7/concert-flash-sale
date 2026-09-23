package com.flashsale.config;

import com.flashsale.entity.TicketTier;
import com.flashsale.mapper.TicketTierMapper;
import com.flashsale.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时把各票档库存从 DB 预载入 Redis
 * （Redis 数据丢失/重启后，重启应用即可恢复）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPreloadRunner implements ApplicationRunner {

    private final TicketTierMapper ticketTierMapper;
    private final StockService stockService;

    @Override
    public void run(ApplicationArguments args) {
        List<TicketTier> tiers = ticketTierMapper.selectList(null);
        tiers.forEach(tier -> {
            stockService.preloadStock(tier.getId(), tier.getStock());
            log.info("preload stock: tierId={} stock={}", tier.getId(), tier.getStock());
        });
    }
}
