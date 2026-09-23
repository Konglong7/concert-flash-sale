package com.flashsale.controller;

import com.flashsale.common.Result;
import com.flashsale.dto.ConcertDetailVO;
import com.flashsale.dto.ConcertVO;
import com.flashsale.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/concert")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping("/list")
    public Result<List<ConcertVO>> list() {
        return Result.ok(concertService.listConcerts());
    }

    @GetMapping("/{id}")
    public Result<ConcertDetailVO> detail(@PathVariable Long id) {
        return Result.ok(concertService.getDetail(id));
    }
}
