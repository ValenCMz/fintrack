package com.valencmz.fintrack.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.valencmz.fintrack.errors.ApiResponse;
import com.valencmz.fintrack.model.dto.report.AccountBalanceResponse;
import com.valencmz.fintrack.model.dto.report.CategoryExpenseResponse;
import com.valencmz.fintrack.model.dto.report.MonthlySummaryResponse;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.service.report.ReportService;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> monthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserAuth userAuth) {
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }
        return ResponseEntity.ok(ApiResponse.success(reportService.monthlySummary(userAuth, year, month)));
    }

    @GetMapping("/by-category")
    public ResponseEntity<ApiResponse<List<CategoryExpenseResponse>>> byCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(reportService.byCategory(userAuth, from, to)));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<List<AccountBalanceResponse>>> balance(
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(reportService.balance(userAuth)));
    }
}
