package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingPlanSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingPlanDO;
import cn.zhicloud.module.qms.dal.mysql.training.TrainingPlanMapper;
import cn.zhicloud.module.qms.enums.qms.TrainingPlanStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：培训计划建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 COMPLETED(40)，造出一份「未排期、未实施、无培训记录」
 * 却已完成的培训计划——培训完成率是岗位资格与内审员资质的前提证据。
 *
 * @author 智云
 */
@Import(TrainingPlanServiceImpl.class)
public class TrainingPlanStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private TrainingPlanServiceImpl trainingPlanService;

    @Resource
    private TrainingPlanMapper trainingPlanMapper;

    @Test
    public void test_createTrainingPlan_ignoresSuppliedCompletedStatus() {
        // 攻击形态：建单即完成（跳过排期与实施）（§12.235.5）
        TrainingPlanSaveReqVO req = new TrainingPlanSaveReqVO();
        req.setPlanNo("TP-BYPASS-COMPLETED-001");
        req.setPlanName("2024 年度质量培训");
        req.setYear(2024);
        req.setStatus(TrainingPlanStatusEnum.COMPLETED.getStatus());

        Long id = trainingPlanService.createTrainingPlan(req);

        TrainingPlanDO saved = trainingPlanMapper.selectById(id);
        assertEquals(TrainingPlanStatusEnum.DRAFT.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createTrainingPlan_ignoresSuppliedScheduledStatus() {
        // 对称检查：任何非 DRAFT 状态都不生效（§12.235.5）
        TrainingPlanSaveReqVO req = new TrainingPlanSaveReqVO();
        req.setPlanNo("TP-BYPASS-SCHEDULED-001");
        req.setPlanName("2024 年度安全培训");
        req.setYear(2024);
        req.setStatus(TrainingPlanStatusEnum.SCHEDULED.getStatus());

        Long id = trainingPlanService.createTrainingPlan(req);

        TrainingPlanDO saved = trainingPlanMapper.selectById(id);
        assertEquals(TrainingPlanStatusEnum.DRAFT.getStatus(), saved.getStatus());
    }
}
