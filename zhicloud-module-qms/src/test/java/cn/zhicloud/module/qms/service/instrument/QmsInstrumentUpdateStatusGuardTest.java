package cn.zhicloud.module.qms.service.instrument;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * §12.236.7：QmsInstrumentServiceImpl **修改**路径的状态护栏测试。
 *
 * <p>本类<b>没有任何流转方法</b>，通用修改是状态唯一的移动方式。
 *
 * <p><b>断言语义（易错点）</b>：MyBatis-Plus 的 updateById 默认<b>跳过 null 字段</b>，
 * 所以服务层 setStatus(null) 的实际效果是「该列不进 UPDATE 语句」，
 * 即<b>保持原值不变</b>，而不是「把库里的值置空」。
 * 因此这里断言「原值未被改写」，写成 assertNull 是错的（踩过一次）。
 *
 * @author 智云
 */
@Import(QmsInstrumentServiceImpl.class)
public class QmsInstrumentUpdateStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private QmsInstrumentServiceImpl service;

    @Resource
    private QmsInstrumentMapper mapper;

    @Test
    public void test_updateInstrumentignoresSuppliedStatus() {
        QmsInstrumentDO existing = randomPojo(QmsInstrumentDO.class, o -> o.setStatus(10));
        mapper.insert(existing);

        QmsInstrumentSaveReqVO req = new QmsInstrumentSaveReqVO();
        req.setId(existing.getId());
        req.setStatus(30);

        service.updateInstrument(req);

        QmsInstrumentDO after = mapper.selectById(existing.getId());
        assertEquals(10, after.getStatus(),
                "调用方传入的终态不得生效：状态只能由流转方法产生（§12.236.7）");
    }
}
