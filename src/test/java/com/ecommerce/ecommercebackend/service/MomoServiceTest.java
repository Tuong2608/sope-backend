package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.config.MomoConfig;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MomoServiceTest {

    private static final String SECRET = "test-momo-secret";
    private MomoService service;

    @BeforeEach
    void setUp() {
        MomoConfig config = new MomoConfig();
        config.setPartnerCode("SOPEMOMO");
        config.setAccessKey("SOPEACCESS");
        config.setSecretKey(SECRET);
        service = new MomoService(config, WebClient.builder());
    }

    @Test
    void verifiesOfficialIpnSignatureOrder() throws Exception {
        Map<String, String> params = new HashMap<>(Map.ofEntries(
                Map.entry("amount", "150000"), Map.entry("extraData", ""),
                Map.entry("message", "Successful."), Map.entry("orderId", "MM123"),
                Map.entry("orderInfo", "Thanh toan SOPE"), Map.entry("orderType", "momo_wallet"),
                Map.entry("partnerCode", "SOPEMOMO"), Map.entry("payType", "qr"),
                Map.entry("requestId", "REQ123"), Map.entry("responseTime", "1721720663942"),
                Map.entry("resultCode", "0"), Map.entry("transId", "4088878653")));
        String raw = "accessKey=SOPEACCESS&amount=150000&extraData=&message=Successful."
                + "&orderId=MM123&orderInfo=Thanh toan SOPE&orderType=momo_wallet"
                + "&partnerCode=SOPEMOMO&payType=qr&requestId=REQ123&responseTime=1721720663942"
                + "&resultCode=0&transId=4088878653";
        params.put("signature", sign(raw));

        assertThat(service.verifySignature(params)).isTrue();
        params.put("amount", "1");
        assertThat(service.verifySignature(params)).isFalse();
    }

    @Test
    void mapsMomoResultGroups() {
        assertThat(service.resolveStatus(Map.of("resultCode", "0"))).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(service.resolveStatus(Map.of("resultCode", "7000"))).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(service.resolveStatus(Map.of("resultCode", "1006"))).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(service.resolveStatus(Map.of("resultCode", "1005"))).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(service.resolveStatus(Map.of("resultCode", "99"))).isEqualTo(PaymentStatus.FAILED);
    }

    private String sign(String raw) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Hex.encodeHexString(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
