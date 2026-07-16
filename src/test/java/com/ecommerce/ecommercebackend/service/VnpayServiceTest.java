package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.config.VnpayConfig;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VnpayServiceTest {

    private static final String SECRET = "test-vnpay-secret";
    private VnpayService service;

    @BeforeEach
    void setUp() {
        VnpayConfig config = new VnpayConfig();
        config.setTmnCode("SOPE01");
        config.setHashSecret(SECRET);
        config.setUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        config.setReturnUrl("https://example.ngrok.app/api/payment/vnpay/callback");
        service = new VnpayService(config, mock(RestTemplate.class));
    }

    @Test
    void createsSignedUrlWithAmountExpiryAndChannel() throws Exception {
        String url = service.createPaymentUrl("VP123", 150_000L, "Thanh toan don hang SOPE",
                "127.0.0.1", "VNBANK", LocalDateTime.of(2026, 7, 17, 12, 30));
        Map<String, String> params = queryParams(url);

        assertThat(params.get("vnp_Amount")).isEqualTo("15000000");
        assertThat(params.get("vnp_TxnRef")).isEqualTo("VP123");
        assertThat(params.get("vnp_BankCode")).isEqualTo("VNBANK");
        assertThat(params.get("vnp_ExpireDate")).isEqualTo("20260717123000");
        assertThat(params.get("vnp_SecureHash")).isEqualTo(independentSignature(params));
        assertThat(service.verifySignature(params)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VNPAYQR", "VNBANK", "INTCARD"})
    void forwardsSupportedBankCode(String channel) throws Exception {
        String url = service.createPaymentUrl("VP" + channel, 150_000L, "Thanh toan SOPE",
                "127.0.0.1", channel, LocalDateTime.of(2026, 7, 17, 12, 30));
        assertThat(queryParams(url).get("vnp_BankCode")).isEqualTo(channel);
    }

    @Test
    void rejectsTamperedSignature() throws Exception {
        String url = service.createPaymentUrl("VP124", 150_000L, "Thanh toan don hang SOPE",
                "127.0.0.1", null, LocalDateTime.of(2026, 7, 17, 12, 30));
        Map<String, String> params = queryParams(url);
        params.put("vnp_Amount", "1");
        assertThat(service.verifySignature(params)).isFalse();
    }

    private Map<String, String> queryParams(String url) throws Exception {
        Map<String, String> params = new HashMap<>();
        for (String pair : new URI(url).getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        return params;
    }

    private String independentSignature(Map<String, String> params) throws Exception {
        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");
        String data = sorted.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return Hex.encodeHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
