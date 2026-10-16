package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.ScarSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.ScarDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.ScarMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * §12.236.7：ScarServiceImpl **修改**路径的状态护栏测试。
 *
 * <p>Scar 有 closeScar，所以通用修改写状态是「绕过流转」而非唯一路径；但仍是洞 —— PUT status=30 即可关单，跳过 closeScar。
 *
 * <p><b>断言语义（易错点）</b>：MyBatis-Plus 的 updateById 默认<b>跳过 null 字段</b>，
 * 所以服务层 setStatus(null) 的实际效果是「该列不进 UPDATE 语句」，
 * 即<b>保持原值不变</b>，而不是「把库里的值置空」。
 * 因此这里断言「原值未被改写」，写成 assertNull 是错的（踩过一次）。
 *
 * @author 智云
 */
@Import(ScarServiceImpl.class)
public class ScarUpdateStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private ScarServiceImpl service;

    @Resource
    private ScarMapper mapper;

    @Test
    public void test_updateScarignoresSuppliedStatus() {
        ScarDO existing = randomPojo(ScarDO.class, o -> o.setStatus(10));
        mapper.insert(existing);

        ScarSaveReqVO req = new ScarSaveReqVO();
        req.setId(existing.getId());
        req.setStatus(30);

        service.updateScar(req);

        ScarDO after = mapper.selectById(existing.getId());
        assertEquals(10, after.getStatus(),
                "调用方传入的终态不得生效：状态只能由流转方法产生（§12.236.7）");
    }
}
