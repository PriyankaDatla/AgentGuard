package com.agentguard.intent;

import com.agentguard.exception.IntentParsingException;
import com.agentguard.policy.PurchaseIntent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
public class GroqIntentService implements IntentService {
    private static final Logger log = LoggerFactory.getLogger(GroqIntentService.class);
    private static final Set<String> ALLOWED_FIELDS = Set.of("category", "preferredBrand", "maxAmount", "requiresCategoryMatch");
    private final GroqChatClient client;
    private final ObjectMapper objectMapper;
    public GroqIntentService(GroqChatClient client, ObjectMapper objectMapper) { this.client = client; this.objectMapper = objectMapper; }
    @Override public PurchaseIntent parseIntent(String userRequest, Long userId) {
        log.info("Intent parsing requested for userId={}", userId);
        String content = client.complete(IntentExtractionPrompt.SYSTEM, userRequest);
        try {
            JsonNode node = objectMapper.readTree(content);
            if (!node.isObject() || node.size() != ALLOWED_FIELDS.size() || !node.fieldNames().hasNext()) throw malformed();
            Set<String> returned = new HashSet<>();
            node.fieldNames().forEachRemaining(returned::add);
            if (!returned.equals(ALLOWED_FIELDS)) throw malformed();
            JsonNode category = node.get("category"), brand = node.get("preferredBrand"), maxAmount = node.get("maxAmount"), categoryMatch = node.get("requiresCategoryMatch");
            if (category == null || !category.isTextual() || category.asText().isBlank() || brand == null || !(brand.isNull() || (brand.isTextual() && !brand.asText().isBlank())) || maxAmount == null || !maxAmount.isNumber() || categoryMatch == null || !categoryMatch.isBoolean()) throw malformed();
            BigDecimal max = maxAmount.decimalValue();
            if (max.signum() <= 0) throw new IntentParsingException("Intent maximum amount must be greater than zero");
            PurchaseIntent intent = new PurchaseIntent(userId, category.asText().trim(), brand.isNull() ? null : brand.asText().trim(), max, categoryMatch.booleanValue());
            log.info("Intent parsing succeeded for userId={}", userId);
            return intent;
        } catch (JsonProcessingException | IllegalArgumentException e) { log.warn("Intent parsing failed for userId={}", userId); throw new IntentParsingException("Intent provider returned malformed structured data", e); }
    }
    private IntentParsingException malformed() { return new IntentParsingException("Intent provider returned an invalid or incomplete purchase intent"); }
}
