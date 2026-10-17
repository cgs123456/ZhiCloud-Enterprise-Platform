package cn.zhicloud.module.qms.service.eightd;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.dal.dataobject.eightd.EightDReportDO;
import cn.zhicloud.module.qms.dal.mysql.eightd.EightDReportMapper;
import cn.zhicloud.module.qms.enums.qms.EightDStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.EIGHT_D_REPORT_NOT_CLOSEABLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link EightDReportServiceImpl} 的单元测试
 *
 * <p>覆盖 8D 报告全流程：创建（默认 DRAFT）→ 逐阶段推进（D1→D7）→ 关闭（D8），
 * 以及异常场景（已关闭不能再推进、不存在校验）。
 *
 * @author 智云
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
        cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO createReqVO =
                new cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO();
        createReqVO.setReportNo("8D20240101001");
        createReqVO.setTitle("产品外观不良 8D 报告");

        // 调用
        Long id = eightDReportService.createEightDReport(createReqVO);

        // 断言：默认状态为草稿
        EightDReportDO report = eightDReportMapper.selectById(id);
        assertNotNull(report);
        assertEquals(EightDStatusEnum.DRAFT.getStatus(), report.getStatus());
    }

    // ------------------------------------------------------------------
    // §12.235.5（critical）：建单状态护栏
    //
    // 8D 的阶段号本身就是「报告推进到第几步」的证据。旧实现只在 status 为 null
    // 时补默认值，调用方即可在**建单**这一步直接传 D8_CLOSED(80)，
    // 造出一份「D1~D7 内容全空」却已关闭的 8D 报告。
    // ------------------------------------------------------------------

    @Test
    public void test_createEightDReport_ignoresSuppliedClosedStage() {
        // 攻击形态：建单即传 D8_CLOSED（跳过 D1~D7 全部阶段内容）（§12.235.5）
        cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO createReqVO =
                new cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO();
        createReqVO.setReportNo("8D-BYPASS-CLOSED-001");
        createReqVO.setTitle("产品外观不良 8D 报告");
        createReqVO.setStatus(EightDStatusEnum.D8_CLOSED.getStatus());

        Long id = eightDReportService.createEightDReport(createReqVO);

        EightDReportDO report = eightDReportMapper.selectById(id);
        assertEquals(EightDStatusEnum.DRAFT.getStatus(), report.getStatus());
        // 关闭时间只能由 closeEightDReport 写入
        assertNull(report.getCloseTime());
    }

    @Test
    public void test_createEightDReport_ignoresSuppliedMidStage() {
        // 对称检查：任何非 DRAFT 阶段都不生效（§12.235.5）
        cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO createReqVO =
                new cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportSaveReqVO();
        createReqVO.setReportNo("8D-BYPASS-D4-001");
        createReqVO.setTitle("尺寸超差 8D 报告");
        createReqVO.setStatus(EightDStatusEnum.D4_ROOT_CAUSE.getStatus());

        Long id = eightDReportService.createEightDReport(createReqVO);

        EightDReportDO report = eightDReportMapper.selectById(id);
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

    // ------------------------------------------------------------------
    // §12.253：关闭类幂等——只有 D7（预防）阶段可关闭。
    //
    // 旧实现无前置态校验：DRAFT/D1~D6 可直关 D8（阶段内容全空却终结，D1-D7 证据被跳过），
    // 已关闭重复关会把 closeTime 二次重写（关闭证据链被变更）。以下钉住收紧后的契约。
    // CAS 分支（读-判-写竞态后 0 行拒绝）需真并发才可触发，单测不可确定性覆盖，在 E2E/ADR 登记。
    // ------------------------------------------------------------------

    @Test
    public void test_closeEightDReport_draftRejected() {
        // DRAFT 不满足前置态：拒绝关闭且状态/关闭时间均未被触碰（拒绝路径零副作用）
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.DRAFT.getStatus());
            o.setCloseTime(null);
        });
        eightDReportMapper.insert(report);

        assertServiceException(() -> eightDReportService.closeEightDReport(report.getId()),
                EIGHT_D_REPORT_NOT_CLOSEABLE);

        EightDReportDO after = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.DRAFT.getStatus(), after.getStatus());
        assertNull(after.getCloseTime());
    }

    @Test
    public void test_closeEightDReport_midStageRejected() {
        // D1~D6 任一中间阶段关闭 = 跳过未完成阶段流转，与 DRAFT 同拒（§12.253）
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.D4_ROOT_CAUSE.getStatus());
            o.setCloseTime(null);
        });
        eightDReportMapper.insert(report);

        assertServiceException(() -> eightDReportService.closeEightDReport(report.getId()),
                EIGHT_D_REPORT_NOT_CLOSEABLE);

        EightDReportDO after = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.D4_ROOT_CAUSE.getStatus(), after.getStatus());
        assertNull(after.getCloseTime());
    }

    @Test
    public void test_closeEightDReport_alreadyClosedRejected() {
        // 已关闭重复关：closeTime 必须保持首次关闭时刻，不被二次重写（§12.253）
        LocalDateTime firstCloseTime = LocalDateTime.of(2024, 11, 20, 15, 30);
        EightDReportDO report = randomPojo(EightDReportDO.class, o -> {
            o.setStatus(EightDStatusEnum.D8_CLOSED.getStatus());
            o.setCloseTime(firstCloseTime);
        });
        eightDReportMapper.insert(report);

        assertServiceException(() -> eightDReportService.closeEightDReport(report.getId()),
                EIGHT_D_REPORT_NOT_CLOSEABLE);

        EightDReportDO after = eightDReportMapper.selectById(report.getId());
        assertEquals(EightDStatusEnum.D8_CLOSED.getStatus(), after.getStatus());
        assertEquals(firstCloseTime, after.getCloseTime());
    }
}
