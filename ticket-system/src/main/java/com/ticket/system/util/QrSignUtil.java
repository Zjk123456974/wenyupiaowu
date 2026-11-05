package com.ticket.system.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * 电子票核销二维码时效签名工具
 *
 * 设计目标：
 * 1. 用户端二维码不再只是“视觉占位”，而是编码一段由后端签名的令牌，
 *    令牌包含 票号 + 核销码 + 过期时间戳，使用 HmacSHA256 防伪造。
 * 2. 令牌有时效（默认 10 分钟），截图被他人拿到也会在过期后失效，降低冒用风险。
 * 3. 验票端扫码后把令牌原样上送，后端先验签再验时效，最后取出真实核销码走原核销逻辑。
 *
 * 令牌格式： base64url(payload) + "." + hex(HmacSHA256(secret, base64url(payload)))
 * payload 格式： ticketNo + "|" + verifyCode + "|" + expEpochMillis
 */
@Component
public class QrSignUtil {

    /** 签名密钥，生产环境务必在 application.yml 中替换为强随机值 */
    @Value("${ticket.qr.secret:CHANGE_ME_QR_SECRET}")
    private String secret;

    /** 二维码有效时长（分钟） */
    @Value("${ticket.qr.ttl-minutes:10}")
    private long ttlMinutes;

    /**
     * 生成签名令牌
     */
    public String sign(String ticketNo, String verifyCode) {
        long exp = Instant.now().plusSeconds(ttlMinutes * 60).toEpochMilli();
        String payload = ticketNo + "|" + verifyCode + "|" + exp;
        String b64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return b64 + "." + hmac(b64);
    }

    /**
     * 校验令牌的签名与时效，全部通过才返回 true
     */
    public boolean verify(String token) {
        if (token == null || token.indexOf('.') < 0) {
            return false;
        }
        int idx = token.lastIndexOf('.');
        String b64 = token.substring(0, idx);
        String sig = token.substring(idx + 1);
        if (!constantTimeEquals(hmac(b64), sig)) {
            return false;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");
            if (parts.length != 3) {
                return false;
            }
            long exp = Long.parseLong(parts[2]);
            return Instant.now().toEpochMilli() < exp;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从令牌中取出真实核销码；若入参不是令牌（不含 "."）则原样返回
     */
    public String extractVerifyCode(String token) {
        if (token == null || token.indexOf('.') < 0) {
            return token;
        }
        int idx = token.lastIndexOf('.');
        String b64 = token.substring(0, idx);
        try {
            String payload = new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");
            if (parts.length == 3) {
                return parts[1];
            }
        } catch (Exception ignored) {
            // 解析失败交由调用方处理
        }
        return null;
    }

    /**
     * 计算令牌剩余有效毫秒数（已过期返回 <=0）
     */
    public long getRemainingMillis(String token) {
        if (token == null || token.indexOf('.') < 0) {
            return 0;
        }
        int idx = token.lastIndexOf('.');
        String b64 = token.substring(0, idx);
        try {
            String payload = new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");
            if (parts.length == 3) {
                long exp = Long.parseLong(parts[2]);
                return exp - Instant.now().toEpochMilli();
            }
        } catch (Exception ignored) {
            // 忽略
        }
        return 0;
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC 签名计算失败", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * 恒定时间比较，避免时序侧信道
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
