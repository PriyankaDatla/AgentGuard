package com.agentguard.payment;

import com.agentguard.exception.PaymentProcessingException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class SdkRazorpayOrderClient implements RazorpayOrderClient {
    private final RazorpayClient razorpayClient;

    SdkRazorpayOrderClient(Optional<RazorpayClient> razorpayClient) {
        this.razorpayClient = razorpayClient.orElse(null);
    }

    @Override
    public String createOrder(long amountInPaise, String currency, String receipt) {
        if (razorpayClient == null) {
            throw new PaymentProcessingException();
        }
        try {
            JSONObject request = new JSONObject();
            request.put("amount", amountInPaise);
            request.put("currency", currency);
            request.put("receipt", receipt);
            Order order = razorpayClient.orders.create(request);
            return order.get("id").toString();
        } catch (RazorpayException | RuntimeException e) {
            throw new PaymentProcessingException();
        }
    }
}
