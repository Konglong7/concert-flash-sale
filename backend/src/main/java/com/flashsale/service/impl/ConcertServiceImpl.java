package com.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.flashsale.common.BusinessException;
import com.flashsale.dto.ConcertDetailVO;
import com.flashsale.dto.ConcertVO;
import com.flashsale.entity.Concert;
import com.flashsale.entity.TicketTier;
import com.flashsale.mapper.ConcertMapper;
import com.flashsale.mapper.TicketTierMapper;
import com.flashsale.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertServiceImpl extends ServiceImpl<ConcertMapper, Concert> implements ConcertService {

    private final TicketTierMapper ticketTierMapper;

    @Override
    public List<ConcertVO> listConcerts() {
        return list(new LambdaQueryWrapper<Concert>()
                        .orderByAsc(Concert::getShowTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ConcertDetailVO getDetail(Long id) {
        Concert concert = getById(id);
        if (concert == null) {
            throw new BusinessException(404, "演出不存在");
        }

        ConcertDetailVO detail = new ConcertDetailVO();
        detail.setId(concert.getId());
        detail.setName(concert.getName());
        detail.setVenue(concert.getVenue());
        detail.setShowTime(concert.getShowTime());
        detail.setSaleStartTime(concert.getSaleStartTime());
        detail.setSaleEndTime(concert.getSaleEndTime());

        int status = computeStatus(concert);
        detail.setStatus(status);
        detail.setCountdownSeconds(computeCountdown(concert, status));

        List<ConcertDetailVO.TierVO> tiers = ticketTierMapper
                .selectList(new LambdaQueryWrapper<TicketTier>()
                        .eq(TicketTier::getConcertId, id)
                        .orderByAsc(TicketTier::getPrice))
                .stream()
                .map(tier -> {
                    ConcertDetailVO.TierVO vo = new ConcertDetailVO.TierVO();
                    vo.setId(tier.getId());
                    vo.setName(tier.getName());
                    vo.setPrice(tier.getPrice());
                    vo.setTotalStock(tier.getTotalStock());
                    vo.setStock(tier.getStock());
                    vo.setLimitPerUser(tier.getLimitPerUser());
                    return vo;
                })
                .toList();
        detail.setTiers(tiers);
        return detail;
    }

    private ConcertVO toVO(Concert concert) {
        ConcertVO vo = new ConcertVO();
        vo.setId(concert.getId());
        vo.setName(concert.getName());
        vo.setVenue(concert.getVenue());
        vo.setShowTime(concert.getShowTime());
        vo.setSaleStartTime(concert.getSaleStartTime());
        vo.setSaleEndTime(concert.getSaleEndTime());
        int status = computeStatus(concert);
        vo.setStatus(status);
        vo.setCountdownSeconds(computeCountdown(concert, status));
        return vo;
    }

    /**
     * 按当前时间实时计算状态，不依赖库中的 status 字段
     */
    private int computeStatus(Concert concert) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(concert.getSaleStartTime())) {
            return Concert.STATUS_NOT_ON_SALE;
        }
        if (now.isAfter(concert.getSaleEndTime())) {
            return Concert.STATUS_ENDED;
        }
        return Concert.STATUS_ON_SALE;
    }

    private long computeCountdown(Concert concert, int status) {
        LocalDateTime now = LocalDateTime.now();
        if (status == Concert.STATUS_NOT_ON_SALE) {
            return Math.max(0, Duration.between(now, concert.getSaleStartTime()).getSeconds());
        }
        if (status == Concert.STATUS_ON_SALE) {
            return Math.max(0, Duration.between(now, concert.getSaleEndTime()).getSeconds());
        }
        return 0;
    }
}
