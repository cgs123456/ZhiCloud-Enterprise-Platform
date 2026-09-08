package cn.zhicloud.module.aimultiagent.enums;

/**
 * 技能分类枚举 - 两级结构
 * <p>
 * Level 1: 大类（如 wms、qms、procurement、sales 等业务域）
 * Level 2: 小类/具体工具（如 receipt_order_list、merchant_list 等具体操作）
 * 用于技能组织、权限控制和统计分析。
 * </p>
 *
 * @author 智云
 */
public enum SkillCategory {

    // === WMS 模块技能（库存发货商户） ===
    WMS("wms", null, "仓储物流模块技能"),
    RECEIPT_ORDER_LIST("wms:receipt_order_list", "wms", "收货单列表"),
    MERCHANT_LIST("wms:merchant_list", "wms", "商户列表"),
    INVENTORY_LIST("wms:inventory_list", "wms", "库存列表"),
    SHIPMENT_ORDER_LIST("wms:shipment_order_list", "wms", "发货单列表"),

    // === QMS 模块技能（质检） ===
    QMS("qms", null, "质量检测模块技能"),
    INSPECTION_ORDER_LIST("qms:inspection_order_list", "qms", "质检单列表"),

    // === 采购模块技能 ===
    PROCUREMENT("procurement", null, "采购模块技能"),

    // === 销售模块技能 ===
    SALES("sales", null, "销售模块技能"),

    // === 报告生成技能 ===
    REPORT("report", null, "报告生成技能"),

    // === AI-RAG 评估技能 ===
    AI_RAG_EVAL("ai_rag_eval", null, "AI RAG 评估模块技能"),
    RAG_EVAL_DATASET("ai_rag_eval:dataset", "ai_rag_eval", "评估数据集管理"),
    RAG_EVAL_BATCH("ai_rag_eval:batch", "ai_rag_eval", "批量评估执行"),
    RAG_EVAL_AGGREGATE("ai_rag_eval:aggregate", "ai_rag_eval", "结果聚合对比");

    /** Level 1: 大类标识 */
    private final String categoryLevel1;

    /** Level 2: 小类/工具标识 */
    private final String categoryLevel2;

    /** 中文描述 */
    private final String description;

    SkillCategory(String categoryLevel2, String categoryLevel1, String description) {
        this.categoryLevel2 = categoryLevel2;
        this.categoryLevel1 = categoryLevel1;
        this.description = description;
    }

    SkillCategory(String categoryLevel2) {
        this.categoryLevel2 = categoryLevel2;
        this.categoryLevel1 = null;
        this.description = null;
    }

    public String getCategoryLevel1() {
        return categoryLevel1;
    }

    public String getCategoryLevel2() {
        return categoryLevel2;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 Level 2 标识获取对应的 Level 1 大类
     */
    public static String getLevel1ByLevel2(String level2) {
        if (level2 == null) {
            return null;
        }
        for (SkillCategory category : values()) {
            if (level2.equals(category.categoryLevel2)) {
                return category.categoryLevel1;
            }
        }
        return null;
    }

    /**
     * 检查给定的工具名是否属于指定的大类
     */
    public static boolean belongsTo(String level2, String categoryLevel1) {
        if (level2 == null || categoryLevel1 == null) {
            return false;
        }
        return categoryLevel1.equals(getLevel1ByLevel2(level2));
    }
}