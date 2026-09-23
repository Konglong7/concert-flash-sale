package com.flashsale.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flashsale.dto.ConcertDetailVO;
import com.flashsale.dto.ConcertVO;
import com.flashsale.entity.Concert;

import java.util.List;

public interface ConcertService extends IService<Concert> {

    /**
     * 演出列表，按开演时间升序
     */
    List<ConcertVO> listConcerts();

    /**
     * 演出详情，含票档与实时状态/倒计时
     */
    ConcertDetailVO getDetail(Long id);
}
