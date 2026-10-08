package cn.zhicloud.framework.mdm.api;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 主数据对照批量登记结果（§12.147）
 *
 * <p>批量登记**逐条独立**：一条失败不影响其余（补全场景里"48 条成功、2 条失败"若整体回滚，
 * 等于让运营重来一遍，正是批量入口要避免的）。故返回结构按条目给结果，而不是单一成功/失败。
 *
 * @author 智云集成治理
 */
@Data
public class MdmCrossRefBatchUpsertResult {

    /** 成功条数（新增或覆盖更新） */
    private int successCount;

    /** 失败条数 */
    private int failureCount;

    /** 逐条结果（与入参同序，便于运营定位第几条出错） */
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {

        /** 入参下标（从 0 起） */
        private int index;

        /** 是否成功 */
        private boolean success;

        /** 成功时的对照主键 */
        private Long id;

        /** 源键摘要，便于对账定位 */
        private String key;

        /** 失败原因（业务异常 message；成功时为 null） */
        private String error;

    }

}
