package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.QualificationDO;
import cn.zhicloud.module.qms.dal.mysql.training.QualificationMapper;
import cn.zhicloud.module.qms.enums.qms.QualificationStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：岗位资格建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 EXPIRED(30)/REVOKED(40)，把「资格状态」这一
 * 上岗判据写成调用方想要的样子；或反过来直接写 VALID 绕过取证与考核留痕。
 * 这里钉住的是：状态只能由到期/撤销等流转产生，建单恒为 VALID。
 *
 * @author 智云
 */
@Import(QualificationServiceImpl.class)
public class QualificationStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private QualificationServiceImpl qualificationService;

    @Resource
    private QualificationMapper qualificationMapper;

    @Test
    public void test_createQualification_ignoresSuppliedRevokedStatus() {
        // 攻击形态：建单即撤销（§12.235.5）
        QualificationSaveReqVO req = new QualificationSaveReqVO();
        req.setUserId(randomLongId());
        req.setUserName("张三");
        req.setQualificationName("内审员资格");
        req.setStatus(QualificationStatusEnum.REVOKED.getStatus());

        Long id = qualificationService.createQualification(req);

        QualificationDO saved = qualificationMapper.selectById(id);
        assertEquals(QualificationStatusEnum.VALID.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createQualification_ignoresSuppliedExpiredStatus() {
        // 对称检查：任何非 VALID 状态都不生效（§12.235.5）
        QualificationSaveReqVO req = new QualificationSaveReqVO();
        req.setUserId(randomLongId());
        req.setUserName("李四");
        req.setQualificationName("焊接资格");
        req.setStatus(QualificationStatusEnum.EXPIRED.getStatus());

        Long id = qualificationService.createQualification(req);

        QualificationDO saved = qualificationMapper.selectById(id);
        assertEquals(QualificationStatusEnum.VALID.getStatus(), saved.getStatus());
    }
}
