package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingRecordDO;
import cn.zhicloud.module.qms.dal.mysql.training.TrainingRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：培训记录建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 20（已完成），造出一份「未参训、无成绩、无考核」
 * 却已完成的培训记录——培训记录是岗位资格取证的输入证据，凭空完成等于伪造。
 *
 * <p>该域尚无状态枚举（DO 注释：10=未开始 / 20=已完成），按「抽象延迟到第二个
 * 真实用例」的原则不新造枚举，测试内保留字面量并注明语义。
 *
 * @author 智云
 */
@Import(TrainingRecordServiceImpl.class)
public class TrainingRecordStatusGuardTest extends BaseDbUnitTest {

    /** 培训记录状态：10 未开始 / 20 已完成（与该域 DO 注释一致） */
    private static final int STATUS_NOT_STARTED = 10;
    private static final int STATUS_COMPLETED = 20;

    @Resource
    private TrainingRecordServiceImpl trainingRecordService;

    @Resource
    private TrainingRecordMapper trainingRecordMapper;

    @Test
    public void test_createTrainingRecord_ignoresSuppliedCompletedStatus() {
        // 攻击形态：建单即完成（跳过实际参训与考核）（§12.235.5）
        TrainingRecordSaveReqVO req = new TrainingRecordSaveReqVO();
        req.setRecordNo("TR-BYPASS-COMPLETED-001");
        req.setTraineeId(randomLongId());
        req.setTraineeName("张三");
        req.setStatus(STATUS_COMPLETED);

        Long id = trainingRecordService.createTrainingRecord(req);

        TrainingRecordDO saved = trainingRecordMapper.selectById(id);
        assertEquals(Integer.valueOf(STATUS_NOT_STARTED), saved.getStatus());
    }

    @Test
    public void test_createTrainingRecord_defaultStatusIsNotStarted() {
        // 未传状态时同样是未开始（§12.235.5）
        TrainingRecordSaveReqVO req = new TrainingRecordSaveReqVO();
        req.setRecordNo("TR-DEFAULT-001");
        req.setTraineeId(randomLongId());
        req.setTraineeName("李四");

        Long id = trainingRecordService.createTrainingRecord(req);

        TrainingRecordDO saved = trainingRecordMapper.selectById(id);
        assertEquals(Integer.valueOf(STATUS_NOT_STARTED), saved.getStatus());
    }
}
