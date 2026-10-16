package cn.zhicloud.module.qms.service.instrument;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentMapper;
import cn.zhicloud.module.qms.enums.instrument.QmsInstrumentCategoryEnum;
import cn.zhicloud.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：计量器具建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 SCRAPPED(30)，让一台在用量具在台账层面「已报废」，
 * 从而绕开校准到期告警与使用追溯；或传 SEALED(40) 制造封存假象。
 *
 * @author 智云
 */
@Import(QmsInstrumentServiceImpl.class)
public class QmsInstrumentStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private QmsInstrumentServiceImpl instrumentService;

    @Resource
    private QmsInstrumentMapper instrumentMapper;

    @Test
    public void test_createInstrument_ignoresSuppliedScrappedStatus() {
        // 攻击形态：建单即报废（绕开校准到期告警与使用追溯）（§12.235.5）
        QmsInstrumentSaveReqVO req = new QmsInstrumentSaveReqVO();
        req.setCode("INS-BYPASS-SCRAPPED-001");
        req.setName("千分尺");
        req.setCategory(QmsInstrumentCategoryEnum.LENGTH.getCategory());
        req.setStatus(QmsInstrumentStatusEnum.SCRAPPED.getStatus());

        Long id = instrumentService.createInstrument(req);

        QmsInstrumentDO saved = instrumentMapper.selectById(id);
        assertEquals(QmsInstrumentStatusEnum.IN_USE.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createInstrument_ignoresSuppliedDisabledStatus() {
        // 对称检查：任何非 IN_USE 状态都不生效（§12.235.5）
        QmsInstrumentSaveReqVO req = new QmsInstrumentSaveReqVO();
        req.setCode("INS-BYPASS-DISABLED-001");
        req.setName("游标卡尺");
        req.setCategory(QmsInstrumentCategoryEnum.LENGTH.getCategory());
        req.setStatus(QmsInstrumentStatusEnum.DISABLED.getStatus());

        Long id = instrumentService.createInstrument(req);

        QmsInstrumentDO saved = instrumentMapper.selectById(id);
        assertEquals(QmsInstrumentStatusEnum.IN_USE.getStatus(), saved.getStatus());
    }
}
