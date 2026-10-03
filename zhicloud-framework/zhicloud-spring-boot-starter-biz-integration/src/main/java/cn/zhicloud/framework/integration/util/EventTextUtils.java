package cn.zhicloud.framework.integration.util;

/**
 * 集成事件文本工具（V100）
 *
 * <p>{@code last_error / error} 列都是 VARCHAR(2000)，超长异常信息必须先截断再落库，
 * 否则 MySQL 严格模式下整条 UPDATE 会失败，反而把「失败原因」和「重试状态」一起丢掉。
 *
 * @author 智云集成治理
 */
public class EventTextUtils {

    /** last_error / error 列宽上限 */
    public static final int MAX_ERROR_LEN = 2000;

    private EventTextUtils() {
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * 截断到列宽上限，避免超长异常信息导致 UPDATE 失败
     */
    public static String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        String value = text.replace('\n', ' ').replace('\r', ' ').trim();
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    /**
     * 取异常根因的一句话描述（含异常类名，便于直接定位）
     */
    public static String rootMessage(Throwable cause) {
        if (cause == null) {
            return null;
        }
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return msg == null ? root.getClass().getName() : root.getClass().getName() + ": " + msg;
    }

}