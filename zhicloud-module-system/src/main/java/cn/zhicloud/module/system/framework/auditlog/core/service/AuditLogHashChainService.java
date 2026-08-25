package cn.zhicloud.module.system.framework.auditlog.core.service;

import cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.system.dal.dataobject.logger.OperateLogDO;
import cn.zhicloud.module.system.framework.auditlog.config.AuditLogProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 审计日志 Hash 链计算与验证服务
 *
 * Hash 链算法：
 * 1. 第一条日志：prevHash = ""，currentHash = SHA256("" + 业务字段)
 * 2. 后续日志：prevHash = 上一条日志的 currentHash，
 *    currentHash = SHA256(prevHash + type + subType + bizId + userId + requestUrl + requestMethod + action + createTime)
 *
 * @author zhicloud
 */
public class AuditLogHashChainService {

    /**
     * Hash 计算中 createTime 的格式（精确到毫秒，避免同秒日志 hash 冲突）
     */
    private static final DateTimeFormatter CREATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final AuditLogProperties properties;

    public AuditLogHashChainService(AuditLogProperties properties) {
        this.properties = properties;
    }

    /**
     * 是否启用 Hash 链式审计
     */
    public boolean isEnabled() {
        return properties.getHashChain().isEnabled();
    }

    /**
     * 计算并填充日志的 prevHash 与 currentHash
     *
     * @param log          当前待插入的日志
     * @param prevHash     上一条日志的 currentHash，若为链上的第一条日志传入 ""（空字符串）
     */
    public void fillHash(OperateLogDO log, String prevHash) {
        if (!isEnabled()) {
            return;
        }
        if (prevHash == null) {
            prevHash = "";
        }
        log.setPrevHash(prevHash);
        log.setCurrentHash(calculateHash(log, prevHash));
    }

    /**
     * 计算单条日志的 hash
     *
     * 计算公式：SHA256(prevHash + type + subType + bizId + userId + requestUrl + requestMethod + action + createTime)
     *
     * @param log       日志对象
     * @param prevHash  上一条日志的 currentHash
     * @return 当前日志的 hash（64 位十六进制小写）
     */
    public String calculateHash(OperateLogDO log, String prevHash) {
        if (prevHash == null) {
            prevHash = "";
        }
        StringBuilder sb = new StringBuilder(256);
        sb.append(prevHash);
        sb.append(nullToEmpty(log.getType()));
        sb.append(nullToEmpty(log.getSubType()));
        sb.append(log.getBizId() == null ? "" : log.getBizId());
        sb.append(log.getUserId() == null ? "" : log.getUserId());
        sb.append(nullToEmpty(log.getRequestUrl()));
        sb.append(nullToEmpty(log.getRequestMethod()));
        sb.append(nullToEmpty(log.getAction()));
        sb.append(log.getCreateTime() == null ? "" : log.getCreateTime().format(CREATE_TIME_FORMATTER));
        return sha256Hex(sb.toString());
    }

    /**
     * 校验单条日志的 currentHash 是否与重新计算的 hash 一致
     *
     * @param log  日志对象（包含 prevHash 与 currentHash）
     * @return true 一致（未被篡改）；false 不一致（已被篡改）
     */
    public boolean verifyHash(OperateLogDO log) {
        if (!isEnabled()) {
            return true;
        }
        if (log.getCurrentHash() == null || log.getCurrentHash().isEmpty()) {
            return false;
        }
        String recomputed = calculateHash(log, log.getPrevHash());
        return recomputed.equals(log.getCurrentHash());
    }

    /**
     * 计算 SHA256 并返回 64 位十六进制小写字符串
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法，理论上不会不存在
            throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, "SHA-256 algorithm not available", e);
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

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 规范化 createTime 用于 hash 计算（暴露给调用方，便于构造测试日志）
     */
    public static String formatCreateTime(LocalDateTime createTime) {
        return createTime == null ? "" : createTime.format(CREATE_TIME_FORMATTER);
    }

}
