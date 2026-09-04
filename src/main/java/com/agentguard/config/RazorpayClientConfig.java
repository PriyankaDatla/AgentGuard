package com.agentguard.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayClientConfig {

    /**
     * The RazorpayClient bean is only created when both credentials are present.
     * When credentials are absent the bean is simply not registered; SdkRazorpayOrderClient
     * detects this at call time and throws PaymentProcessingException instead of
     * preventing the application from starting.
     */
    @Bean
    @ConditionalOnProperty(prefix = "razorpay", name = {"key-id", "key-secret"})
    RazorpayClient razorpayClient(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret) throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
