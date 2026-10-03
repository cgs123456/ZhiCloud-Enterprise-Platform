package cn.zhicloud.framework.mdm.enums;

/**
 * 主数据跨域映射常量（V101）
 *
 * <p>枚举值刻意用字符串常量而不是 Java 枚举：{@code mdm_cross_ref} 是数据表驱动的字典，
 * 新增业务域（MES/QMS/TMS/CRM）时只需插入新行，不需要改 Java 代码发版。
 * 代价是失去了编译期校验，由「映射缺失必须抛错」的运行时行为补偿——
 * 宁可让集成链路显式失败进入死信，也不要静默按错误的 ID 写下游单据。
 *
 * @author 智云集成治理
 */
public final class MdmConstants {

    private MdmConstants() {
    }

    // ========== 系统标识 ==========
    public static final String SYSTEM_ERP = "ERP";
    public static final String SYSTEM_WMS = "WMS";
    public static final String SYSTEM_MES = "MES";
    public static final String SYSTEM_QMS = "QMS";
    public static final String SYSTEM_TMS = "TMS";
    public static final String SYSTEM_CRM = "CRM";

    // ========== 实体类型 ==========
    public static final String ENTITY_PRODUCT = "PRODUCT";
    public static final String ENTITY_SKU = "SKU";
    public static final String ENTITY_WAREHOUSE = "WAREHOUSE";
    public static final String ENTITY_CUSTOMER = "CUSTOMER";
    public static final String ENTITY_MERCHANT = "MERCHANT";

    // ========== 映射状态 ==========
    public static final String STATUS_VALID = "VALID";
    public static final String STATUS_INVALID = "INVALID";

}