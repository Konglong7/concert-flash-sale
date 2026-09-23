package com.flashsale.controller;

import com.flashsale.common.BusinessException;
import com.flashsale.common.Result;
import com.flashsale.dto.SeckillRequest;
import com.flashsale.dto.SeckillResultVO;
import com.flashsale.entity.Order;
import com.flashsale.service.OrderService;
import com.flashsale.service.SeckillService;
import com.flashsale.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 抢票/订单接口（需 JWT）
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final SeckillService seckillService;
    private final OrderService orderService;

    /**
     * 抢票：校验 + Redis 原子扣减 + 发 MQ，立即返回"排队中"，
     * 结果通过 /result/{tierId} 轮询获取
     */
    @PostMapping("/seckill")
    public Result<Void> seckill(@Valid @RequestBody SeckillRequest request) {
        seckillService.seckill(UserContext.getUserId(), request);
        return Result.ok();
    }

    /**
     * 轮询抢票结果：processing / success(含订单号) / fail(含原因) / none
     */
    @GetMapping("/seckill/result/{tierId}")
    public Result<SeckillResultVO> result(@PathVariable Long tierId) {
        Long userId = UserContext.getUserId();
        String raw = seckillService.getResult(userId, tierId);
        SeckillResultVO vo = parseResult(raw);
        return Result.ok(vo);
    }

    private SeckillResultVO parseResult(String raw) {
        if (raw == null) {
            return new SeckillResultVO("none", null, null);
        }
        if (raw.startsWith("SUCCESS:")) {
            return new SeckillResultVO("success", raw.substring(8), null);
        }
        if (raw.startsWith("FAIL:")) {
            return new SeckillResultVO("fail", null, raw.substring(5));
        }
        return new SeckillResultVO("processing", null, null);
    }

    /**
     * 【压测对照】传统方案：请求线程内同步扣 DB 库存 + 建订单（无 Redis 预扣、无 MQ 削峰）
     * 仅用于与 /seckill 的 Redis+MQ 方案做性能对比
     */
    @PostMapping("/direct")
    public Result<Void> direct(@Valid @RequestBody SeckillRequest request) {
        Long userId = UserContext.getUserId();
        orderService.createOrder(new com.flashsale.mq.SeckillMessage(
                userId, request.getConcertId(), request.getTierId(), request.getIdCard()));
        return Result.ok();
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/list")
    public Result<List<Order>> list() {
        return Result.ok(orderService.listByUser(UserContext.getUserId()));
    }

    /**
     * 支付订单（模拟支付：直接置为已支付）
     */
    @PostMapping("/{orderNo}/pay")
    public Result<Order> pay(@PathVariable String orderNo) {
        return Result.ok(orderService.pay(UserContext.getUserId(), orderNo));
    }
}
