package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.service.SellerService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Called directly by Stripe, not through api-gateway's JWT-authenticated routes —
// Webhook.constructEvent's HMAC signature check IS the auth here, not a bearer token.
// Whatever exposes this route publicly (gateway route config, external config-server)
// needs to permitAll() it, the same way GET /api/v1/products/** is permitted today.
@Slf4j
@RestController
@RequestMapping("/api/v1/sellers/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final SellerService sellerService;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Rejected Stripe webhook with invalid signature");
            return ResponseEntity.badRequest().build();
        }

        if ("account.updated".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject().ifPresent(stripeObject -> {
                if (stripeObject instanceof Account account) {
                    sellerService.handleStripeAccountUpdated(
                            account.getId(),
                            Boolean.TRUE.equals(account.getChargesEnabled()),
                            Boolean.TRUE.equals(account.getPayoutsEnabled())
                    );
                }
            });
        }

        return ResponseEntity.ok().build();
    }
}
