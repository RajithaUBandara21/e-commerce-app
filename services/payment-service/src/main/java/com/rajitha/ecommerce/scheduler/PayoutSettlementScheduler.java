package com.rajitha.ecommerce.scheduler;

import com.rajitha.ecommerce.service.SellerPayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Daily safety net — POST /api/v1/payouts/settle (admin-triggered) calls the exact
// same SellerPayoutService.settlePendingPayouts() for on-demand settlement; this
// just means an admin doesn't have to remember to trigger it manually.
@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutSettlementScheduler {

    private final SellerPayoutService sellerPayoutService;

    @Scheduled(cron = "${platform.payout-settlement-cron:0 0 3 * * *}")
    public void settlePendingPayouts() {
        log.info("Running scheduled payout settlement");
        sellerPayoutService.settlePendingPayouts();
    }
}
