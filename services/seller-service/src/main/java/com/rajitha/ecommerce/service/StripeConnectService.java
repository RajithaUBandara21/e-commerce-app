package com.rajitha.ecommerce.service;

public interface StripeConnectService {

    String createExpressAccount(String email);

    String createAccountLink(String stripeAccountId, String refreshUrl, String returnUrl);
}
