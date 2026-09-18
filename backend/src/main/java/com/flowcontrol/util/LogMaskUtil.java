package com.flowcontrol.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.regex.Pattern;

/**
 * 日志脱敏工具
 * 对敏感数据进行脱敏处理，防止敏感信息泄露到日志中
 *
 * 脱敏策略：
 * - 手机号：138****5678
 * - 邮箱：a***@example.com
 * - IP地址：192.168.***.**
 * - 身份证：110***********1234
 * - 银行卡：**** **** **** 1234
 */
@Slf4j
public class LogMaskUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1})\\w+(@\\w+\\.\\w+)");
    private static final Pattern IP_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.)\\d{1,3}(\\.\\d{1,3})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{3})\\d{11}(\\d{4})");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{8,12}(\\d{4})");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("([Pp]assword[=：:]?)\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("([Tt]oken[=：:]?)\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_KEY_PATTERN = Pattern.compile("([Aa]pi[_-]?[Kk]ey[=：:]?)\\S+", Pattern.CASE_INSENSITIVE);

    /**
     * 脱敏手机号：138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    /**
     * 脱敏邮箱：a***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        return EMAIL_PATTERN.matcher(email).replaceAll("$1***$2");
    }

    /**
     * 脱敏IP地址：192.168.***.**
     */
    public static String maskIp(String ip) {
        if (ip == null) return ip;
        return IP_PATTERN.matcher(ip).replaceAll("$1***$2");
    }

    /**
     * 脱敏身份证号：110***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) return idCard;
        return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1***********$2");
    }

    /**
     * 脱敏银行卡号：**** **** **** 1234
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 12) return bankCard;
        return BANK_CARD_PATTERN.matcher(bankCard).replaceAll("**** **** **** $2");
    }

    /**
     * 脱敏密码
     */
    public static String maskPassword(String logLine) {
        if (logLine == null) return logLine;
        return PASSWORD_PATTERN.matcher(logLine).replaceAll("$1******");
    }

    /**
     * 脱敏Token
     */
    public static String maskToken(String logLine) {
        if (logLine == null) return logLine;
        return TOKEN_PATTERN.matcher(logLine).replaceAll("$1******");
    }

    /**
     * 脱敏API Key
     */
    public static String maskApiKey(String logLine) {
        if (logLine == null) return logLine;
        return API_KEY_PATTERN.matcher(logLine).replaceAll("$1******");
    }

    /**
     * 脱敏日志行（自动识别并脱敏多种敏感信息）
     */
    public static String mask(String logLine) {
        if (logLine == null) return logLine;
        String masked = maskPassword(logLine);
        masked = maskToken(masked);
        masked = maskApiKey(masked);
        return masked;
    }

    /**
     * 设置MDC上下文（用于请求追踪）
     */
    public static void setRequestContext(String requestId, String userId, String clientIp) {
        MDC.put("requestId", requestId);
        if (userId != null) {
            MDC.put("userId", userId);
        }
        if (clientIp != null) {
            MDC.put("clientIp", maskIp(clientIp));
        }
    }

    /**
     * 清除MDC上下文
     */
    public static void clearRequestContext() {
        MDC.clear();
    }
}
