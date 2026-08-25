package cn.zhicloud.module.oa.service.reimburse;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimbursePageReqVO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimburseSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import cn.zhicloud.module.oa.dal.mysql.reimburse.OaReimburseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OaReimburseServiceImpl} 的单元测试
 *
 * @author 智云
 */
@Import(OaReimburseServiceImpl.class)
public class OaReimburseServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OaReimburseServiceImpl reimburseService;

    @Resource
    private OaReimburseMapper reimburseMapper;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    @Test
    public void test_createReimburse_success() {
        // 准备参数
        OaReimburseSaveReqVO reqVO = new OaReimburseSaveReqVO();
        reqVO.setNo("RB001");
        reqVO.setReimburseName("差旅报销");
        reqVO.setTotalAmount(new BigDecimal("1500.00"));
        reqVO.setReimburseType(1);

        // 调用
        Long id = reimburseService.createReimburse(reqVO);

        // 校验
        OaReimburseDO result = reimburseMapper.selectById(id);
        assertNotNull(result);
        assertEquals("RB001", result.getNo());
        assertEquals("差旅报销", result.getReimburseName());
    }

    @Test
    public void test_createReimburse_duplicateNo() {
        // mock 数据
        OaReimburseDO exist = randomPojo(OaReimburseDO.class, o -> o.setNo("RB002"));
        reimburseMapper.insert(exist);

        // 准备参数
        OaReimburseSaveReqVO reqVO = new OaReimburseSaveReqVO();
        reqVO.setNo("RB002");
        reqVO.setReimburseName("测试");

        // 调用并校验异常
        assertThrows(Exception.class, () -> reimburseService.createReimburse(reqVO));
    }

    @Test
    public void test_updateReimburse_success() {
        // mock 数据
        OaReimburseDO reimburse = randomPojo(OaReimburseDO.class, o -> {
            o.setNo("RB003");
            o.setReimburseName("旧名称");
            o.setStatus(10); // 草稿，update 仅允许草稿状态
        });
        reimburseMapper.insert(reimburse);

        // 准备参数
        OaReimburseSaveReqVO reqVO = new OaReimburseSaveReqVO();
        reqVO.setId(reimburse.getId());
        reqVO.setNo("RB003");
        reqVO.setReimburseName("新名称");
        reqVO.setTotalAmount(new BigDecimal("2000.00"));

        // 调用
        reimburseService.updateReimburse(reqVO);

        // 校验
        OaReimburseDO result = reimburseMapper.selectById(reimburse.getId());
        assertEquals("新名称", result.getReimburseName());
    }

    @Test
    public void test_deleteReimburse_success() {
        // mock 数据
        OaReimburseDO reimburse = randomPojo(OaReimburseDO.class);
        reimburseMapper.insert(reimburse);

        // 调用
        reimburseService.deleteReimburse(reimburse.getId());

        // 校验
        assertNull(reimburseMapper.selectById(reimburse.getId()));
    }

    @Test
    public void test_deleteReimburse_notExists() {
        assertThrows(Exception.class, () -> reimburseService.deleteReimburse(randomLongId()));
    }

    @Test
    public void test_submitReimburse_success() {
        // mock 数据
        OaReimburseDO reimburse = randomPojo(OaReimburseDO.class, o -> {
            o.setStatus(10); // 草稿 STATUS_DRAFT=10
        });
        reimburseMapper.insert(reimburse);

        // 调用
        reimburseService.submitReimburse(reimburse.getId());

        // 校验状态变更
        OaReimburseDO result = reimburseMapper.selectById(reimburse.getId());
        assertNotEquals(0, result.getStatus());
    }

    @Test
    public void test_getReimburse_success() {
        // mock 数据
        OaReimburseDO reimburse = randomPojo(OaReimburseDO.class);
        reimburseMapper.insert(reimburse);

        // 调用并校验
        OaReimburseDO result = reimburseService.getReimburse(reimburse.getId());
        assertNotNull(result);
        assertEquals(reimburse.getId(), result.getId());
    }

}
