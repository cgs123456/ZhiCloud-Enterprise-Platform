package cn.zhicloud.module.qms.service.msa;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.msa.vo.MsaStudySaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.msa.MsaStudyDO;
import cn.zhicloud.module.qms.dal.mysql.msa.MsaStudyMapper;
import cn.zhicloud.module.qms.enums.qms.MsaStatusEnum;
import cn.zhicloud.module.qms.enums.qms.MsaStudyTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：MSA 研究建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 COMPLETED(30)，造出一份「无测量数据、未算 GR&R」
 * 却已完成的分析研究——GR&R 结论是测量系统可信度的判据，凭空产生等于伪造结论。
 *
 * <p>本类与 {@link MsaStudyServiceImplTest}（Mockito 桩接 Mapper，覆盖 GR&R 算法）
 * 职责不同：这里走 H2 真实落库，只钉 create 的写入护栏。
 *
 * @author 智云
 */
@Import(MsaStudyServiceImpl.class)
public class MsaStudyCreateStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private MsaStudyServiceImpl msaStudyService;

    @Resource
    private MsaStudyMapper msaStudyMapper;

    @Test
    public void test_createMsaStudy_ignoresSuppliedCompletedStatus() {
        // 攻击形态：建单即完成（跳过测量数据与 GR&R 计算）（§12.235.5）
        MsaStudySaveReqVO req = new MsaStudySaveReqVO();
        req.setStudyNo("MSA-BYPASS-COMPLETED-001");
        req.setStudyType(MsaStudyTypeEnum.GAGE_RR.getStudyType());
        req.setStatus(MsaStatusEnum.COMPLETED.getStatus());

        Long id = msaStudyService.createMsaStudy(req);

        MsaStudyDO saved = msaStudyMapper.selectById(id);
        assertEquals(MsaStatusEnum.DRAFT.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createMsaStudy_ignoresSuppliedInProgressStatus() {
        // 对称检查：任何非 DRAFT 状态都不生效（§12.235.5）
        MsaStudySaveReqVO req = new MsaStudySaveReqVO();
        req.setStudyNo("MSA-BYPASS-INPROGRESS-001");
        req.setStudyType(MsaStudyTypeEnum.GAGE_RR.getStudyType());
        req.setStatus(MsaStatusEnum.IN_PROGRESS.getStatus());

        Long id = msaStudyService.createMsaStudy(req);

        MsaStudyDO saved = msaStudyMapper.selectById(id);
        assertEquals(MsaStatusEnum.DRAFT.getStatus(), saved.getStatus());
    }
}
