package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.ScarSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.ScarDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.ScarMapper;
import cn.zhicloud.module.qms.enums.qms.ScarStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：SCAR 建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 CLOSED(30)，造出一份「供应商从未回复、纠正措施为空」
 * 却已关闭的 SCAR。闭环证据（供应商整改回复）凭空产生。
 *
 * @author 智云
 */
@Import(ScarServiceImpl.class)
public class ScarStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private ScarServiceImpl scarService;

    @Resource
    private ScarMapper scarMapper;

    @Test
    public void test_createScar_ignoresSuppliedClosedStatus() {
        // 攻击形态：建单即关闭（跳过供应商整改闭环）（§12.235.5）
        ScarSaveReqVO req = new ScarSaveReqVO();
        req.setScarNo("SCAR-BYPASS-CLOSED-001");
        req.setSupplierId(randomLongId());
        req.setDefectDescription("来料尺寸超差");
        req.setStatus(ScarStatusEnum.CLOSED.getStatus());

        Long id = scarService.createScar(req);

        ScarDO saved = scarMapper.selectById(id);
        assertEquals(ScarStatusEnum.OPEN.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createScar_ignoresSuppliedInProgressStatus() {
        // 对称检查：任何非 OPEN 状态都不生效（§12.235.5）
        ScarSaveReqVO req = new ScarSaveReqVO();
        req.setScarNo("SCAR-BYPASS-INPROGRESS-001");
        req.setSupplierId(randomLongId());
        req.setDefectDescription("来料外观不良");
        req.setStatus(ScarStatusEnum.IN_PROGRESS.getStatus());

        Long id = scarService.createScar(req);

        ScarDO saved = scarMapper.selectById(id);
        assertEquals(ScarStatusEnum.OPEN.getStatus(), saved.getStatus());
    }
}
