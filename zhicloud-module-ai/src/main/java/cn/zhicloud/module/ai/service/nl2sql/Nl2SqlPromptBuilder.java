package cn.zhicloud.module.ai.service.nl2sql;

/**
 * NL2SQL Prompt 构建器
 *
 * <p>构建发送给 LLM 的 prompt，包含：
 * <ul>
 *   <li>系统角色：SQL 专家</li>
 *   <li>数据库 schema 元数据（表名、列名、注释）</li>
 *   <li>安全约束（只读 SELECT、禁止 DDL/DML、多租户隔离、性能要求）</li>
 *   <li>返回格式（纯 SQL，不要 markdown 代码块）</li>
 * </ul>
 *
 * @author zhicloud
 */
public class Nl2SqlPromptBuilder {

    /**
     * 默认 schema 元数据（可按 dataSource 切换）
     *
     * <p>列举常用业务表的核心字段，供 LLM 生成 SQL 时参考。
     * 实际生产环境可通过外部配置或元数据查询动态注入。
     */
    private static final String DEFAULT_SCHEMA_METADATA = """
            // ERP 销售订单
            erp_sale_order(id, no, customer_id, total_amount, order_date, status, tenant_id, creator, create_time, deleted)
            // ERP 销售订单明细
            erp_sale_order_item(id, order_id, product_id, count, total_price, tenant_id)
            // ERP 产品
            erp_product(id, name, category_id, unit_id, status, tenant_id)
            // ERP 客户
            erp_customer(id, name, level, contact, tenant_id)
            // MES 工单
            mes_pro_work_order(id, work_order_no, product_id, quantity, status, plan_start_date, plan_end_date, tenant_id)
            // MES 设备
            mes_dv_machinery(id, code, name, status, tenant_id)
            // WMS 库存
            wms_inventory(id, product_id, warehouse_id, qty, tenant_id)
            // 系统用户
            system_users(id, username, nickname, status, tenant_id)
            """;

    /**
     * 构建系统 prompt
     *
     * @param schemaMetadata 数据库 schema 元数据，为空时使用默认元数据
     * @return 系统 prompt
     */
    public String buildSystemPrompt(String schemaMetadata) {
        String schema = (schemaMetadata == null || schemaMetadata.isBlank())
                ? DEFAULT_SCHEMA_METADATA : schemaMetadata;
        return """
                你是一个 SQL 专家。根据用户的自然语言问题，生成 MySQL 8 兼容的只读 SELECT 查询。

                数据库 schema：
                %s

                约束：
                1. 只能生成 SELECT 语句（或 WITH ... SELECT）
                2. 禁止 DDL/DML（INSERT/UPDATE/DELETE/DROP/ALTER/CREATE/TRUNCATE 等）
                3. 必须包含 tenant_id 条件（多租户隔离，固定写法：tenant_id = ${tenantId}，由调用方替换）
                4. 必须包含 deleted = 0 条件（逻辑删除过滤）
                5. 性能：避免全表扫描，使用索引；大数据量表必须加 LIMIT（默认 LIMIT 1000）
                6. 日期范围使用标准 MySQL 函数（如 DATE_FORMAT、DATE_SUB、NOW()）

                返回格式：纯 SQL，不要 markdown 代码块，不要任何解释说明。
                """.formatted(schema);
    }

    /**
     * 构建用户 prompt
     *
     * @param naturalLanguage 用户的自然语言问题
     * @return 用户 prompt
     */
    public String buildUserPrompt(String naturalLanguage) {
        return "自然语言问题：" + naturalLanguage;
    }

    /**
     * 清理 LLM 返回的 SQL（去除 markdown 代码块、多余空白）
     *
     * @param raw LLM 原始返回
     * @return 清理后的纯 SQL
     */
    public String cleanSql(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String sql = raw.trim();
        // 去除 markdown 代码块
        if (sql.startsWith("```")) {
            // 去掉首行 ```sql 或 ```
            int firstNewline = sql.indexOf('\n');
            if (firstNewline > 0) {
                sql = sql.substring(firstNewline + 1);
            }
            // 去掉结尾 ```
            int lastFence = sql.lastIndexOf("```");
            if (lastFence >= 0) {
                sql = sql.substring(0, lastFence);
            }
            sql = sql.trim();
        }
        return sql;
    }

}
