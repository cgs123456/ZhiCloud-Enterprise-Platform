package cn.zhicloud.module.iot.framework.tdengine.core;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * TDEngine 表字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TDengineTableField {

    /**
     * 字段名 - TDengine 默认 ts 字段，默认会被 TDengine 创建
     */
    public static final String FIELD_TS = "ts";

    public static final String TYPE_TINYINT = "TINYINT";
    public static final String TYPE_INT = "INT";
    public static final String TYPE_FLOAT = "FLOAT";
    public static final String TYPE_DOUBLE = "DOUBLE";
    public static final String TYPE_BOOL = "BOOL";
    public static final String TYPE_NCHAR = "NCHAR";
    public static final String TYPE_VARCHAR = "VARCHAR";
    public static final String TYPE_TIMESTAMP = "TIMESTAMP";

    /**
     * 字段长度 - VARCHAR 默认长度
     */
    public static final int LENGTH_VARCHAR = 1024;

    /**
     * 注释 - TAG 字段
     */
    public static final String NOTE_TAG = "TAG";

    /**
     * 字段名
     */
    private String field;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 字段长度
     */
    private Integer length;

    /**
     * 注释
     */
    private String note;

    public TDengineTableField(String field, String type) {
        this.field = field;
        this.type = type;
    }

    /**
     * 构建字段名：TDengine 默认会将字段名转为小写，需要和建表、写入、查询保持一致。
     * 例如：PT -> pt，PfT -> pf_t。
     */
    public static String buildFieldName(String field) {
        if (StrUtil.isBlank(field)) {
            return field;
        }
        return StrUtil.toUnderlineCase(field).toLowerCase(Locale.ROOT);
    }

    /**
     * 标识符白名单正则：[a-zA-Z_][a-zA-Z0-9_]*，防止 ${} 拼接 SQL 注入
     */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * 校验 ID：必须为正整数，防止通过 ${} 拼接引入 SQL 注入
     *
     * @param id   ID 值
     * @param name 参数名（用于异常信息，如 productId / deviceId）
     */
    public static void validateId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("非法 " + name + "：" + id + "，必须为正整数");
        }
    }

    /**
     * 校验标识符：必须匹配 [a-zA-Z_][a-zA-Z0-9_]*，防止通过 ${} 拼接引入 SQL 注入
     *
     * <p>用于校验 TDengine 表名、字段名、属性 key 等通过 ${} 拼接到 SQL 的标识符。
     * 注意：本方法校验的是原始标识符，不替代 {@link #buildFieldName(String)} 的大小写转换。
     *
     * @param identifier 标识符（字段名、属性 key、表名片段等）
     */
    public static void validateIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("非法标识符：" + identifier + "，仅允许字母/数字/下划线");
        }
    }
}
