package com.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 条件更新为已支付（status 0 -> 1），保证幂等
     */
    @Update("UPDATE orders SET status = 1, paid_at = NOW() WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int markPaid(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    /**
     * 条件更新为超时（status 0 -> 3），保证幂等
     */
    @Update("UPDATE orders SET status = 3 WHERE order_no = #{orderNo} AND status = 0")
    int markTimeout(@Param("orderNo") String orderNo);
}
