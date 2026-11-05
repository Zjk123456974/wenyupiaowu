package com.ticket.system.util;

import java.util.regex.Pattern;

/**
 * 身份证号码校验工具类
 */
public class IdCardUtil {

    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");

    /** 每位加权因子 */
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /** 校验码对应值 */
    private static final char[] CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 归一化身份证号：去除首尾与内部空白、全角空格，并将校验位统一为大写 X。
     * 防止 “x/X”“全角空格”等写法差异绕过同一身份证的判重逻辑。
     */
    public static String normalize(String idCard) {
        if (idCard == null) {
            return null;
        }
        String normalized = idCard.trim()
                .replace('\u3000', ' ')
                .replaceAll("\\s", "")
                .toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 校验身份证号码格式是否合法
     */
    public static boolean isValid(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return false;
        }
        // 计算校验码
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHT[i];
        }
        char expectedCheckCode = CHECK_CODE[sum % 11];
        char actualCheckCode = Character.toUpperCase(idCard.charAt(17));
        return expectedCheckCode == actualCheckCode;
    }

    /**
     * 从身份证号中提取性别 1-男 2-女
     */
    public static Integer getGender(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return null;
        }
        int genderNum = idCard.charAt(16) - '0';
        return genderNum % 2 == 1 ? 1 : 2;
    }

    /**
     * 从身份证号中提取出生日期（格式：yyyy-MM-dd）
     */
    public static String getBirthday(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return null;
        }
        return idCard.substring(6, 10) + "-" + idCard.substring(10, 12) + "-" + idCard.substring(12, 14);
    }

    /**
     * 身份证号脱敏处理（保留前6后4）
     */
    public static String mask(String idCard) {
        idCard = normalize(idCard);
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        StringBuilder sb = new StringBuilder(idCard.substring(0, 6));
        for (int i = 6; i < idCard.length() - 4; i++) {
            sb.append('*');
        }
        sb.append(idCard.substring(idCard.length() - 4));
        return sb.toString();
    }
}
