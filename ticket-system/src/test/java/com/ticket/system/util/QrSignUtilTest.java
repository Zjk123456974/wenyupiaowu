package com.ticket.system.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QrSignUtil 单元测试：覆盖核销二维码时效签名的核心逻辑
 * - 正常签名令牌可验签且在有效期内
 * - 过期令牌被拒绝
 * - 篡改签名的令牌被拒绝
 * - 使用不同密钥签发的令牌被拒绝
 * - 旧版永久核销码（无 "."）可原样透传，保证向后兼容
 */
class QrSignUtilTest {

    private QrSignUtil util;

    @BeforeEach
    void setUp() {
        util = new QrSignUtil();
        ReflectionTestUtils.setField(util, "secret", "CHANGE_ME_QR_SECRET");
        ReflectionTestUtils.setField(util, "ttlMinutes", 10L);
    }

    @Test
    void signThenVerify_shouldPass() {
        String token = util.sign("T20270001", "abc123uuid");
        assertTrue(util.verify(token), "合法令牌应当通过验签");
        assertEquals("abc123uuid", util.extractVerifyCode(token), "应从令牌中取出真实核销码");
        assertTrue(util.getRemainingMillis(token) > 0, "有效期内的令牌剩余时间应大于 0");
    }

    @Test
    void expiredToken_shouldFail() {
        // 把有效期设为负数，使生成的令牌立即过期
        ReflectionTestUtils.setField(util, "ttlMinutes", -1L);
        String token = util.sign("T20270001", "abc123uuid");
        assertFalse(util.verify(token), "过期令牌应当被拒绝");
    }

    @Test
    void tamperedSignature_shouldFail() {
        String token = util.sign("T20270001", "abc123uuid");
        // 篡改签名部分最后一个字符
        int dot = token.lastIndexOf('.');
        StringBuilder sb = new StringBuilder(token);
        char last = sb.charAt(sb.length() - 1);
        sb.setCharAt(sb.length() - 1, last == 'a' ? 'b' : 'a');
        String tampered = sb.toString();
        assertFalse(util.verify(tampered), "签名被篡改的令牌应当被拒绝");
    }

    @Test
    void wrongSecret_shouldFail() {
        String token = util.sign("T20270001", "abc123uuid");
        QrSignUtil other = new QrSignUtil();
        ReflectionTestUtils.setField(other, "secret", "AnotherSecretKey");
        ReflectionTestUtils.setField(other, "ttlMinutes", 10L);
        assertFalse(other.verify(token), "使用不同密钥验签应当失败");
    }

    @Test
    void legacyPlainCode_shouldPassThrough() {
        // 旧版永久核销码不含 "."，应保持向后兼容，原样返回
        assertEquals("plain-uuid-001", util.extractVerifyCode("plain-uuid-001"));
        // 但纯文本不是令牌，verify 应返回 false（需走原有逻辑或提示无效）
        assertFalse(util.verify("plain-uuid-001"), "纯文本不是合法令牌，验签应失败");
    }
}
