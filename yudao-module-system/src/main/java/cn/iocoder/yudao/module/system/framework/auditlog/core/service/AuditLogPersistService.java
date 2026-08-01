package cn.iocoder.yudao.module.system.framework.auditlog.core.service;

import cn.iocoder.yudao.module.system.dal.dataobject.logger.OperateLogDO;
import cn.iocoder.yudao.module.system.framework.auditlog.config.AuditLogProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志独立存储服务
 *
 * 当 independent-storage.enabled=true 时，将日志以 JSON Lines 格式追加写入到独立文件：
 *   路径  ：{independent-storage.path}/audit-yyyy-MM-dd.jsonl
 *   每行  ：一条日志的 JSON
 *
 * 文件级 Hash 链：
 *   - 每天一个 JSONL 文件
 *   - 文件末尾追加一条 file_hash 记录：
 *     {"type":"file_hash","date":"yyyy-MM-dd","hash":"SHA256(文件所有行内容)","prev_file_hash":"前一天的 hash"}
 *   - 这样即使删除某行，后续文件的 hash 也会不匹配
 *
 * 失败降级：写入异常仅打印日志，不影响主流程
 *
 * @author yudao
 */
@Slf4j
public class AuditLogPersistService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String FILE_HASH_RECORD_TYPE = "file_hash";

    private static final String HASH_CHAIN_FILE_NAME = ".hash-chain";

    private final AuditLogProperties properties;

    /**
     * 单例 ObjectMapper，避免重复创建
     */
    private final ObjectMapper objectMapper;

    public AuditLogPersistService(AuditLogProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 是否启用独立文件存储
     */
    public boolean isEnabled() {
        return properties.getIndependentStorage().isEnabled();
    }

    /**
     * 将一条操作日志追加写入到当天的 JSONL 文件
     *
     * 失败降级：异常仅打印 error 日志，不抛出
     *
     * @param operateLog 操作日志
     */
    public void persist(OperateLogDO operateLog) {
        if (!isEnabled() || operateLog == null) {
            return;
        }
        try {
            String dateStr = resolveDateStr(operateLog);
            Path filePath = resolveAuditFilePath(dateStr);
            ensureDir(filePath.getParent());

            // 写入日志行
            String jsonLine = objectMapper.writeValueAsString(operateLog);
            Files.writeString(filePath, jsonLine + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ex) {
            // 独立存储失败不影响主流程
            log.error("[persist][写入审计日志独立存储失败 logId={}]", operateLog.getId(), ex);
        }
    }

    /**
     * 在当天的 JSONL 文件末尾追加一条 file_hash 记录，并更新 .hash-chain 索引文件
     *
     * 文件级 hash 链：
     *   hash = SHA256(文件所有行内容，包括此前追加的所有日志行，但不包括本次将要追加的 file_hash 行)
     *   prev_file_hash = .hash-chain 文件中上一条记录的 hash（即前一天的 file_hash）
     *
     * 失败降级：异常仅打印 error 日志，不抛出
     *
     * @param date 当天日期
     */
    public void appendFileHash(LocalDate date) {
        if (!isEnabled() || date == null) {
            return;
        }
        try {
            String dateStr = date.format(DATE_FORMATTER);
            Path filePath = resolveAuditFilePath(dateStr);
            if (!Files.exists(filePath)) {
                // 当天没有日志文件，跳过
                return;
            }

            // 1. 读取文件所有行内容
            byte[] fileBytes = Files.readAllBytes(filePath);
            String fileHash = sha256Hex(new String(fileBytes, StandardCharsets.UTF_8));

            // 2. 读取前一天的 file_hash（从 .hash-chain 文件最后一行获取）
            String prevFileHash = readLastFileHash();

            // 3. 追加 file_hash 记录到 JSONL 末尾
            Map<String, Object> hashRecord = new LinkedHashMap<>();
            hashRecord.put("type", FILE_HASH_RECORD_TYPE);
            hashRecord.put("date", dateStr);
            hashRecord.put("hash", fileHash);
            hashRecord.put("prev_file_hash", prevFileHash == null ? "" : prevFileHash);
            String hashRecordLine = objectMapper.writeValueAsString(hashRecord);
            Files.writeString(filePath, hashRecordLine + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // 4. 写入 .hash-chain 索引文件（每行一个 file_hash 记录的 JSON）
            Path hashChainPath = resolveHashChainFilePath();
            Files.writeString(hashChainPath, hashRecordLine + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ex) {
            // 独立存储失败不影响主流程
            log.error("[appendFileHash][写入文件级 hash 链失败 date={}]", date, ex);
        }
    }

    /**
     * 解析审计日志的日期字符串
     *
     * 优先使用 operateLog.getCreateTime()，否则用当天日期
     */
    private String resolveDateStr(OperateLogDO operateLog) {
        if (operateLog.getCreateTime() != null) {
            return operateLog.getCreateTime().format(DATE_FORMATTER);
        }
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * 计算审计文件路径：{path}/audit-yyyy-MM-dd.jsonl
     */
    private Path resolveAuditFilePath(String dateStr) {
        String dir = resolveStorageDir();
        String fileName = "audit-" + dateStr + ".jsonl";
        return Paths.get(dir, fileName);
    }

    /**
     * 计算 .hash-chain 文件路径：{path}/.hash-chain
     */
    private Path resolveHashChainFilePath() {
        String dir = resolveStorageDir();
        return Paths.get(dir, HASH_CHAIN_FILE_NAME);
    }

    /**
     * 解析配置的存储目录
     *
     * 配置项 value 形如 "${AUDIT_LOG_PATH:logs/audit}"，
     * Spring 已经会自动解析占位符为实际值（环境变量优先，否则默认 logs/audit）。
     */
    private String resolveStorageDir() {
        String path = properties.getIndependentStorage().getPath();
        if (path == null || path.isEmpty()) {
            path = "logs/audit";
        }
        // 兼容配置项未经过 Spring 占位符解析的场景：手动剥除 ${...:default} 形式
        if (path.startsWith("${") && path.endsWith("}")) {
            int colonIdx = path.indexOf(':');
            if (colonIdx > 0 && colonIdx < path.length() - 1) {
                path = path.substring(colonIdx + 1, path.length() - 1);
            } else {
                // 没有默认值，直接用环境变量名（一般不会到这里）
                path = "logs/audit";
            }
        }
        return path;
    }

    private void ensureDir(Path dir) throws IOException {
        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    /**
     * 读取 .hash-chain 文件中最后一条记录的 hash（即前一天的 file_hash）
     */
    private String readLastFileHash() throws IOException {
        Path hashChainPath = resolveHashChainFilePath();
        if (!Files.exists(hashChainPath)) {
            return "";
        }
        // 读取最后一行非空记录
        java.util.List<String> lines = Files.readAllLines(hashChainPath, StandardCharsets.UTF_8);
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> record = objectMapper.readValue(line, Map.class);
                Object hash = record.get("hash");
                if (hash != null) {
                    return hash.toString();
                }
            } catch (Throwable ignored) {
                // 跳过解析失败的行
            }
        }
        return "";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法，理论上不会不存在
            throw new IllegalStateException("SHA-256 algorithm not available", e);
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

}
