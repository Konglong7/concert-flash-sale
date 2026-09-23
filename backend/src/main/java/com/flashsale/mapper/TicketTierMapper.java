package com.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.entity.TicketTier;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TicketTierMapper extends BaseMapper<TicketTier> {

    /**
     * 原子扣库存：WHERE stock > 0 保证 DB 层面 0 超卖（双保险，配合 Redis Lua）
     * 返回受影响行数：0 表示库存不足
     */
    @Update("UPDATE ticket_tier SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int decreaseStock(@Param("id") Long id);

    /**
     * 回补库存（订单超时/取消时使用）
     */
    @Update("UPDATE ticket_tier SET stock = stock + 1 WHERE id = #{id}")
    int increaseStock(@Param("id") Long id);
}
