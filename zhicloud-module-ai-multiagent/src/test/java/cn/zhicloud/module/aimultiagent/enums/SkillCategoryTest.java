package cn.zhicloud.module.aimultiagent.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SkillCategory} 单元测试：覆盖 Level-2 → Level-1 归属判定。
 *
 * @author zhicloud
 */
class SkillCategoryTest {

    @Test
    void getLevel1ByLevel2_knownTool_returnsGroup() {
        assertEquals("wms", SkillCategory.getLevel1ByLevel2("wms:receipt_order_list"));
        assertEquals("qms", SkillCategory.getLevel1ByLevel2("qms:inspection_order_list"));
        assertEquals("ai_rag_eval", SkillCategory.getLevel1ByLevel2("ai_rag_eval:batch"));
    }

    @Test
    void getLevel1ByLevel2_unknownOrNull_returnsNull() {
        assertNull(SkillCategory.getLevel1ByLevel2("unknown:tool"));
        assertNull(SkillCategory.getLevel1ByLevel2(null));
    }

    @Test
    void belongsTo_matchesDeclaration() {
        assertTrue(SkillCategory.belongsTo("wms:merchant_list", "wms"));
        assertFalse(SkillCategory.belongsTo("wms:merchant_list", "qms"));
        assertFalse(SkillCategory.belongsTo(null, "wms"));
        assertFalse(SkillCategory.belongsTo("wms:merchant_list", null));
    }

}
