package cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * §12.249（A 组第 6 项）：检验记录 RespVO 回填 severity。
 *
 * <p>背景：§12.239.5 登记「检验记录 RespVO 无 {@code severity}」，
 * 前端 submit 弹窗因此只能把每条已存在记录的严重度默认成「严重」（20），
 * 提交前需人工确认。补上字段后回填必须**精确**，不得被任何默认值改写。</p>
 *
 * <p>severity 取值域由 {@code InspectionRecordSaveReqVO} 的
 * {@code @InEnum(InspectionSeverityEnum)} 约束为 {@code [10, 20, 30]}
 * （致命 / 严重 / 轻微，真机实测错误信息「取值必须为: [10, 20, 30]」）——
 * 本类因此用枚举值而非任意数字做断言。</p>
 *
 * @author 智云
 */
public class QmsInspectionRecordSeverityVOTest {

    @Test
    public void test_severityEnumValues_carriedOverPrecisely() {
        InspectionRecordDO record = new InspectionRecordDO();
        record.setItemId(7L);
        record.setSeverity(30); // 轻微

        InspectionRecordRespVO vo = BeanUtils.toBean(record, InspectionRecordRespVO.class);

        assertEquals(30, vo.getSeverity());
    }

    @Test
    public void test_severityLowestEnumValue_notRewritten() {
        InspectionRecordDO record = new InspectionRecordDO();
        record.setItemId(8L);
        record.setSeverity(10); // 致命：最小合法值，回填不得被任何下限/默认值改写

        InspectionRecordRespVO vo = BeanUtils.toBean(record, InspectionRecordRespVO.class);

        assertEquals(10, vo.getSeverity());
    }

    @Test
    public void test_severityNull_staysNull() {
        InspectionRecordDO record = new InspectionRecordDO();
        record.setItemId(9L);

        InspectionRecordRespVO vo = BeanUtils.toBean(record, InspectionRecordRespVO.class);

        assertNull(vo.getSeverity(), "空值必须保持 null，由前端决定回退值，后端不编造");
    }
}
