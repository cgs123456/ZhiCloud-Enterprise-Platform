package cn.iocoder.yudao.module.qms.service.eightd;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.qms.dal.dataobject.eightd.EightDReportDO;
import cn.iocoder.yudao.module.qms.dal.mysql.eightd.EightDReportMapper;
import cn.iocoder.yudao.module.qms.enums.qms.EightDStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link EightDReportServiceImpl} 的单元测试
 *
 * <p>覆盖 8D 报告全流程：创建（默认 DRAFT）→ 逐阶段推进（D1→D7）→ 关闭（D8），
 * 以及异常场景（已关闭不能再推进、不存在校验）。
 *
 * @author 芋道源码
 */
@Import(EightDReportServiceImpl.class)
public class EightDReportServiceImplTest extends BaseDbUnitTest {

    @Resource
    private EightDReportServiceImpl eightDReportService;

    @Resource
    private EightDReportMapper eightDReportMapper;

    @Test
    public void test_createEightDReport_defaultStatus() {
        // 准备数据
        cn.iocoder.yudao.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO createReqVO =
                new cn.iocoder.yudao.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO();
        createReqVO.setReportNo("8D20240101001");
        createReqVO.setTitle("产品外观不良 8D 报告");

        // 调用
        Long id = eightDReportService.createEightDReport(createReqVO);

        // 断言：默认状态为草稿
        EightDReportDO report = eightDReportMapper.selectById(id);
        assertNotNull(report);
        assertEquals(EightDStatusEnum.DRAFT.getStatus(), report.getStatus());
    }

    @Test
    public void test_advanceStage_fromDraft() {
        // 准备数据：DRAFT 状态
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.DRAFT.getStatus());
        });
        eightDReportMapper.insert(report);

        // 调用：推进到 D1
        eightDReportService.advanceStage(report.getId());

        // 断言
        EightDReportDO updated = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.D1_TEAM.getStatus(), updated.getStatus());
    }

    @Test
    public void test_advanceStage_d1ToD2() {
        // 准备数据：D1 状态
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.D1_TEAM.getStatus());
        });
        eightDReportMapper.insert(report);

        // 调用
        eightDReportService.advanceStage(report.getId());

        // 断言
        EightDReportDO updated = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.D2_PROBLEM.getStatus(), updated.getStatus());
    }

    @Test
    public void test_advanceStage_d6ToD7() {
        // 准备数据：D6 状态
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.D6_IMPLEMENT.getStatus());
        });
        eightDReportMapper.insert(report);

        // 调用
        eightDReportService.advanceStage(report.getId());

        // 断言：D6 → D7
        EightDReportDO updated = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.D7_PREVENT.getStatus(), updated.getStatus());
    }

    @Test
    public void test_advanceStage_d7StaysD7() {
        // 准备数据：D7 状态（已经是 advanceStage 的最高阶段）
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.D7_PREVENT.getStatus());
        });
        eightDReportMapper.insert(report);

        // 调用：D7 推进不会超过 D7（需用 closeEightDReport 到 D8）
        eightDReportService.advanceStage(report.getId());

        // 断言：仍为 D7
        EightDReportDO updated = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.D7_PREVENT.getStatus(), updated.getStatus());
    }

    @Test
    public void test_advanceStage_d8Closed_throwsException() {
        // 准备数据：D8 已关闭
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.D8_CLOSED.getStatus());
        });
        eightDReportMapper.insert(report);

        // 调用：已关闭不能推进，应抛异常
        assertThrows(Exception.class, () -> eightDReportService.advanceStage(report.getId()));
    }

    @Test
    public void test_closeEightDReport_success() {
        // 准备数据：D7 状态
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.D7_PREVENT.getStatus());
        });
        eightDReportMapper.insert(report);

        // 调用：关闭
        eightDReportService.closeEightDReport(report.getId());

        // 断言
        EightDReportDO updated = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.D8_CLOSED.getStatus(), updated.getStatus());
        assertNotNull(updated.getCloseTime());
    }

    @Test
    public void test_closeEightDReport_notExists() {
        assertThrows(Exception.class, () -> eightDReportService.closeEightDReport(randomLongId()));
    }

    @Test
    public void test_deleteEightDReport_notExists() {
        assertThrows(Exception.class, () -> eightDReportService.deleteEightDReport(randomLongId()));
    }

    @Test
    public void test_advanceStage_notExists() {
        assertThrows(Exception.class, () -> eightDReportService.advanceStage(randomLongId()));
    }

}
