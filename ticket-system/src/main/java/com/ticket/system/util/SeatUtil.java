package com.ticket.system.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 选座工具：座位编码的解析与序列化（座位编码仅含字母、数字与 “-”，安全）
 * 编码格式：{sectionId}-{row}-{col}，如 "A-1-2"
 */
public class SeatUtil {

    /**
     * 解析订单座位编码，支持 JSON 数组（["A-1-2"]）与逗号分隔两种存储格式
     */
    public static List<String> parseSeats(String seats) {
        if (seats == null || seats.trim().isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        String s = seats.trim();
        if (s.startsWith("[")) {
            int i = 0;
            int n = s.length();
            StringBuilder cur = null;
            while (i < n) {
                char c = s.charAt(i);
                if (c == '"') {
                    if (cur == null) {
                        cur = new StringBuilder();
                    } else {
                        if (cur.length() > 0) {
                            result.add(cur.toString());
                        }
                        cur = null;
                    }
                } else if (cur != null) {
                    cur.append(c);
                }
                i++;
            }
        } else {
            for (String part : s.split(",")) {
                if (!part.trim().isEmpty()) {
                    result.add(part.trim());
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 将座位编码列表序列化为 JSON 数组字符串
     */
    public static String toJsonArray(List<String> list) {
        if (list == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
