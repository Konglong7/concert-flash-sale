package com.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.entity.UserTicketLimit;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface UserTicketLimitMapper extends BaseMapper<UserTicketLimit> {

    /**
     * 购买数 +1，不存在则插入（原子 upsert，防并发重复建行）
     */
    @Insert("""
            INSERT INTO user_ticket_limit (user_id, concert_id, purchased_count)
            VALUES (#{userId}, #{concertId}, 1)
            ON DUPLICATE KEY UPDATE purchased_count = purchased_count + 1
            """)
    int increasePurchased(@Param("userId") Long userId, @Param("concertId") Long concertId);

    /**
     * 订单超时/取消时回退限购计数（不低于 0）
     */
    @Insert("UPDATE user_ticket_limit SET purchased_count = GREATEST(purchased_count - 1, 0) " +
            "WHERE user_id = #{userId} AND concert_id = #{concertId}")
    int decreasePurchased(@Param("userId") Long userId, @Param("concertId") Long concertId);
}
