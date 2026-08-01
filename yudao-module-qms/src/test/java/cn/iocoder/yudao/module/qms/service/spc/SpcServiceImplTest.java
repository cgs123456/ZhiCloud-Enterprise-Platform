package cn.iocoder.yudao.module.qms.service.spc;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.qms.controller.admin.spc.vo.SamplingPlanRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.spc.vo.SpcAnalysisRespVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionitem.InspectionItemDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionitem.InspectionItemMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * {@link SpcServiceImpl} 的单元测试（Mockito 桩接 Mapper）
 *
 * @author yudao
 */
public class SpcServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private SpcServiceImpl spcService;

    @Mock
    private InspectionItemMapper inspectionItemMapper;
    @Mock
    private InspectionRecordMapper inspectionRecordMapper;

    private InspectionRecordDO record(Long itemId, String value) {
        InspectionRecordDO r = new InspectionRecordDO();
        r.setItemId(itemId);
        r.setMeasuredValue(value);
        return r;
    }

    @Test
    public void testAnalyze_itemNotExists() {
        when(inspectionItemMapper.selectById(1L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> spcService.analyze(1L));
        assertEquals(ErrorCodeConstants.INSPECTION_ITEM_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testAnalyze_normal() {
        InspectionItemDO item = new InspectionItemDO();
        item.setId(1L);
        item.setUpperLimit(new BigDecimal("12"));
        item.setLowerLimit(new BigDecimal("8"));
        item.setTarget("10");
        when(inspectionItemMapper.selectById(1L)).thenReturn(item);

        List<InspectionRecordDO> records = new ArrayList<>();
        records.add(record(1L, "9"));
        records.add(record(1L, "10"));
        records.add(record(1L, "11"));
        records.add(record(1L, "10"));
        records.add(record(1L, "9"));
        records.add(record(1L, "10"));
        records.add(record(1L, "11"));
        records.add(record(1L, "10"));
        // 非本项目的记录（应被 itemId 校验过滤）
        records.add(record(2L, "99"));
        // 非数值文本应被跳过
        records.add(record(1L, "合格"));
        when(inspectionRecordMapper.selectListByItemId(1L)).thenReturn(records);

        SpcAnalysisRespVO resp = spcService.analyze(1L);
        assertNotNull(resp);
        assertEquals(8, resp.getSampleCount());
        // mean = (9+10+11+10+9+10+11+10)/8 = 80/8 = 10
        assertEquals(10.0, resp.getMean().doubleValue(), 0.0001);
        // Cp = (12-8) / (6 * stdDev) ≈ 0.8818；Cpk = min(Cpu, Cpl) ≈ 0.8818
        assertEquals(0.8818, resp.getCp().doubleValue(), 0.005);
        assertEquals(0.8818, resp.getCpk().doubleValue(), 0.005);
        // 全部样本落在 3σ 控制限内
        assertEquals(0, resp.getOutOfControlCount());
        // Cpk < 1.0 -> 不足
        assertEquals("不足", resp.getCapabilityLevel());
        assertNotNull(resp.getRuleViolations());
    }

    @Test
    public void testAnalyze_insufficientSamples() {
        InspectionItemDO item = new InspectionItemDO();
        item.setId(1L);
        item.setUpperLimit(new BigDecimal("12"));
        item.setLowerLimit(new BigDecimal("8"));
        when(inspectionItemMapper.selectById(1L)).thenReturn(item);
        when(inspectionRecordMapper.selectListByItemId(1L)).thenReturn(List.of(record(1L, "10")));

        SpcAnalysisRespVO resp = spcService.analyze(1L);
        assertEquals(1, resp.getSampleCount());
        assertEquals(0.0, resp.getMean().doubleValue(), 0.0001);
        assertEquals(0.0, resp.getStdDev().doubleValue(), 0.0001);
    }

    @Test
    public void testGetSamplingPlan_lotSizeInvalid() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> spcService.getSamplingPlan(0L, "II", BigDecimal.ONE));
        assertEquals(ErrorCodeConstants.SPC_LOT_SIZE_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testGetSamplingPlan_normal() {
        // lotSize=200 落在 MIL-STD-105E 批量区间 151~280，一般检验水平 II -> 字码 G -> 样本量 32
        // AQL=1.0 正常 Ac/Re=3/4
        SamplingPlanRespVO resp = spcService.getSamplingPlan(200L, "II", new BigDecimal("1.0"));
        assertEquals("G", resp.getCodeLetter());
        assertEquals(32, resp.getSampleSize());
        assertEquals(3, resp.getNormalAccept());
        assertEquals(4, resp.getNormalReject());
        // 加严：Ac/Re=2/3
        assertEquals(2, resp.getTightenedAccept());
        assertEquals(3, resp.getTightenedReject());
        // 放宽（AQL×1.5，非加严）与正常一致
        assertEquals(3, resp.getReducedAccept());
        assertEquals(4, resp.getReducedReject());
        assertEquals("II", resp.getInspectionLevel());
    }

    @Test
    public void testGetSamplingPlan_defaultLevel() {
        // 未传水平 -> 默认 II；lotSize=50 -> 字码 D -> 样本量 8
        SamplingPlanRespVO resp = spcService.getSamplingPlan(50L, null, null);
        assertEquals("D", resp.getCodeLetter());
        assertEquals(8, resp.getSampleSize());
    }
}
