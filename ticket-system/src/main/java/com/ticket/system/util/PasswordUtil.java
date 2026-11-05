package com.ticket.system.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希工具。
 *
 * <p>历史版本直接使用无盐 MD5，彩虹表可秒级反查，属于典型安全缺陷。
 * 现统一改为 PBKDF2-HMAC-SHA256（JDK 自带，无需额外依赖）：
 * <pre>pbkdf2$迭代次数$盐(Base64)$哈希(Base64)</pre>
 *
 * <p>为了不破坏已有账号，{@link #matches} 仍兼容 32 位十六进制的旧 MD5；
 * {@link #needsUpgrade} 用于在登录成功时把旧哈希静默升级为新算法。
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2$";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * 生成带随机盐的 PBKDF2 哈希。
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS);
        Base64.Encoder enc = Base64.getEncoder();
        return PREFIX + ITERATIONS + "$" + enc.encodeToString(salt) + "$" + enc.encodeToString(hash);
    }

    /**
     * 校验原始密码与库中哈希是否匹配（兼容旧 MD5）。
     */
    public static boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null || stored.isEmpty()) {
            return false;
        }
        if (stored.startsWith(PREFIX)) {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations);
                return MessageDigest.isEqual(expected, actual);
            } catch (RuntimeException e) {
                return false;
            }
        }
        // 旧数据：无盐 MD5（32 位十六进制）
        if (stored.length() == 32 && stored.matches("[0-9a-fA-F]{32}")) {
            return stored.equalsIgnoreCase(md5Hex(rawPassword));
        }
        return false;
    }

    /**
     * 是否需要升级为新的哈希格式（旧 MD5 或无算法标识的哈希）。
     */
    public static boolean needsUpgrade(String stored) {
        return stored == null || !stored.startsWith(PREFIX);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        }
    }

    private static String md5Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }
}
