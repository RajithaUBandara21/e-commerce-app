package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.service.StripeConnectService;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.stereotype.Service;

// "Separate charges and transfers" Connect model (see PLAN.md): the platform charges
// the customer directly (payment-service, unchanged), and this is only the seller-
// onboarding half — creating the Express account a later Transfer can pay out to.
@Service
public class StripeConnectServiceImpl implements StripeConnectService {

    @Override
    public String createExpressAccount(String email) {
        try {
            var params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setEmail(email)
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                            .setRequested(true).build())
                                    .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                                            .setRequested(true).build())
                                    .build())
                    .build();
            return Account.create(params).getId();
        } catch (StripeException e) {
            throw new IllegalStateException("Failed to create Stripe Connect account: " + e.getMessage(), e);
        }
    }

    @Override
    public String createAccountLink(String stripeAccountId, String refreshUrl, String returnUrl) {
        try {
            var params = AccountLinkCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            return AccountLink.create(params).getUrl();
        } catch (StripeException e) {
            throw new IllegalStateException("Failed to create Stripe account link: " + e.getMessage(), e);
        }
    }
}
