package com.gongyeon.io.netkim.controller;

import com.gongyeon.io.netkim.model.entity.PerformanceEntity;
import com.gongyeon.io.netkim.model.service.PerformanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-prf")
public class PerformanceController {
    private final PerformanceService performanceService;
    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }
    
    // 저장된 공연 전체 조회 메서드
    @GetMapping("")
    public ResponseEntity<List<PerformanceEntity>> getAllPerformance() {
        List<PerformanceEntity> performanceList = performanceService.getAllPerformance();
        if(performanceList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(performanceList, HttpStatus.OK);
    }
    
    // 공연 정보 상세 조회 메서드
    @GetMapping("/{prfId}")
    public ResponseEntity<PerformanceEntity> getPerformanceById(@PathVariable("prfId") long prfId) {
        PerformanceEntity performance = performanceService.getDetail(prfId);
        return new ResponseEntity<>(performance, HttpStatus.OK);
    }
    
    // 공연 정보 추가 메서드
    @PostMapping("")
    public int addPerformance() {
        return 0;
    }
}
