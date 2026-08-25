package cn.zhicloud.module.qms.service.msa;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zhicloud.module.qms.controller.admin.msa.vo.MsaGageRRRespVO;
import cn.zhicloud.module.qms.dal.dataobject.msa.MsaMeasurementDO;
import cn.zhicloud.module.qms.dal.dataobject.msa.MsaStudyDO;
import cn.zhicloud.module.qms.dal.mysql.msa.MsaMeasurementMapper;
import cn.zhicloud.module.qms.dal.mysql.msa.MsaStudyMapper;
import cn.zhicloud.module.qms.enums.ErrorCodeConstants;
import cn.zhicloud.module.qms.enums.qms.MsaStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MsaStudyServiceImpl} 的单元测试（Mockito 桩接 Mapper）
 *
 * <p>重点覆盖 {@link MsaStudyServiceImpl#calculateGageRR(Long)} 的均值极差法（Xbar-R）GR&R 计算，
 * 以及各异常分支（研究不存在 / 数据不足 / 参数非法）。
 *
 * @author zhicloud
 */
public class MsaStudyServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private MsaStudyServiceImpl msaStudyService;

    @Mock
    private MsaStudyMapper msaStudyMapper;
    @Mock
    private MsaMeasurementMapper msaMeasurementMapper;

    private MsaMeasurementDO measurement(Long studyId, Long appraiserId, Long partId, String value) {
        MsaMeasurementDO m = new MsaMeasurementDO();
        m.setStudyId(studyId);
        m.setAppraiserId(appraiserId);
        m.setPartId(partId);
        m.setMeasurementValue(new BigDecimal(value));
        return m;
    }

    private MsaStudyDO buildStudy() {
        MsaStudyDO study = new MsaStudyDO();
        study.setId(1L);
        study.setTrialCount(2);
        study.setPartCount(3);
        study.setAppraiserCount(2);
        return study;
    }

    @Test
    public void testCalculateGageRR_normal() {
        when(msaStudyMapper.selectById(1L)).thenReturn(buildStudy());
        List<MsaMeasurementDO> measurements = new ArrayList<>();
        // 评价人 1
        measurements.add(measurement(1L, 1L, 1L, "10.1"));
        measurements.add(measurement(1L, 1L, 1L, "10.2"));
        measurements.add(measurement(1L, 1L, 2L, "10.5"));
        measurements.add(measurement(1L, 1L, 2L, "10.6"));
        measurements.add(measurement(1L, 1L, 3L, "9.9"));
        measurements.add(measurement(1L, 1L, 3L, "10.0"));
        // 评价人 2
        measurements.add(measurement(1L, 2L, 1L, "10.0"));
        measurements.add(measurement(1L, 2L, 1L, "10.1"));
        measurements.add(measurement(1L, 2L, 2L, "10.4"));
        measurements.add(measurement(1L, 2L, 2L, "10.5"));
        measurements.add(measurement(1L, 2L, 3L, "9.8"));
        measurements.add(measurement(1L, 2L, 3L, "9.9"));
        when(msaMeasurementMapper.selectListByStudyId(1L)).thenReturn(measurements);

        MsaGageRRRespVO resp = msaStudyService.calculateGageRR(1L);
        assertNotNull(resp);
        // 变异分量均应为正
        assertTrue(resp.getEv().doubleValue() > 0, "EV 应 > 0");
        assertTrue(resp.getGageRR().doubleValue() > 0, "Gage R&R 应 > 0");
        assertTrue(resp.getPv().doubleValue() > 0, "PV 应 > 0");
        assertTrue(resp.getTv().doubleValue() > 0, "TV 应 > 0");
        // 不变量：EV <= GageR&R <= TV，且 PV <= TV
        assertTrue(resp.getGageRR().compareTo(resp.getEv()) >= 0);
        assertTrue(resp.getTv().compareTo(resp.getGageRR()) >= 0);
        assertTrue(resp.getTv().compareTo(resp.getPv()) >= 0);
        // 百分比应落在 (0, 100]
        assertTrue(resp.getPercentGageRR().doubleValue() > 0 && resp.getPercentGageRR().doubleValue() <= 100);
        assertTrue(resp.getPercentPV().doubleValue() > 0 && resp.getPercentPV().doubleValue() <= 100);
        // 结论必为三种之一
        assertTrue(List.of("可接受", "有条件接受", "不可接受").contains(resp.getConclusion()));
        assertEquals(Integer.valueOf(measurements.size()), resp.getSampleCount());

        // 计算完成后应把研究状态更新为已完成
        ArgumentCaptor<MsaStudyDO> captor = ArgumentCaptor.forClass(MsaStudyDO.class);
        verify(msaStudyMapper).updateById(captor.capture());
        assertEquals(MsaStatusEnum.COMPLETED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testCalculateGageRR_studyNotExists() {
        when(msaStudyMapper.selectById(1L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> msaStudyService.calculateGageRR(1L));
        assertEquals(ErrorCodeConstants.MSA_STUDY_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testCalculateGageRR_noMeasurements() {
        when(msaStudyMapper.selectById(1L)).thenReturn(buildStudy());
        when(msaMeasurementMapper.selectListByStudyId(1L)).thenReturn(List.of());
        ServiceException ex = assertThrows(ServiceException.class, () -> msaStudyService.calculateGageRR(1L));
        assertEquals(ErrorCodeConstants.MSA_STUDY_DATA_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    public void testCalculateGageRR_invalidCounts() {
        MsaStudyDO bad = buildStudy();
        bad.setTrialCount(0); // 非法：试验次数为 0
        when(msaStudyMapper.selectById(1L)).thenReturn(bad);
        when(msaMeasurementMapper.selectListByStudyId(1L)).thenReturn(List.of(measurement(1L, 1L, 1L, "10.0")));
        ServiceException ex = assertThrows(ServiceException.class, () -> msaStudyService.calculateGageRR(1L));
        assertEquals(ErrorCodeConstants.MSA_STUDY_DATA_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    public void testGetMsaStudy_and_getMeasurementData_delegate() {
        MsaStudyDO study = buildStudy();
        when(msaStudyMapper.selectById(2L)).thenReturn(study);
        assertEquals(study, msaStudyService.getMsaStudy(2L));

        List<MsaMeasurementDO> list = List.of(measurement(2L, 1L, 1L, "10.0"));
        when(msaMeasurementMapper.selectListByStudyId(2L)).thenReturn(list);
        assertEquals(list, msaStudyService.getMeasurementData(2L));
    }
}
