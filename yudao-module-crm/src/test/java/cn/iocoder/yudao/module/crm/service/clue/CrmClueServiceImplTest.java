package cn.iocoder.yudao.module.crm.service.clue;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.CrmCluePageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.CrmClueSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.clue.CrmClueDO;
import cn.iocoder.yudao.module.crm.dal.mysql.clue.CrmClueMapper;
import cn.iocoder.yudao.module.crm.service.customer.CrmCustomerService;
import cn.iocoder.yudao.module.crm.service.followup.CrmFollowUpRecordService;
import cn.iocoder.yudao.module.crm.service.permission.CrmOwnerRecordService;
import cn.iocoder.yudao.module.crm.service.permission.CrmPermissionService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link CrmClueServiceImpl} 的单元测试
 *
 * @author 芋道源码
 */
@Import(CrmClueServiceImpl.class)
public class CrmClueServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmClueServiceImpl clueService;

    @Resource
    private CrmClueMapper clueMapper;

    @MockitoBean
    private CrmCustomerService customerService;
    @MockitoBean
    private CrmPermissionService crmPermissionService;
    @MockitoBean
    private CrmFollowUpRecordService followUpRecordService;
    @MockitoBean
    private CrmOwnerRecordService ownerRecordService;
    @MockitoBean
    private CrmCluePoolConfigService cluePoolConfigService;
    @MockitoBean
    private AdminUserApi adminUserApi;

    @Test
    public void test_createClue_success() {
        // mock
        when(adminUserApi.getUser(any())).thenReturn(new AdminUserRespDTO());

        // 准备参数
        CrmClueSaveReqVO reqVO = new CrmClueSaveReqVO();
        reqVO.setName("测试线索");
        reqVO.setOwnerUserId(1L);
        reqVO.setMobile("13800138000");

        // 调用
        Long id = clueService.createClue(reqVO);

        // 校验
        CrmClueDO result = clueMapper.selectById(id);
        assertNotNull(result);
        assertEquals("测试线索", result.getName());
        verify(crmPermissionService).createPermission(any());
    }

    @Test
    public void test_deleteClue_success() {
        // mock 数据
        CrmClueDO clue = randomPojo(CrmClueDO.class, o -> {
            o.setOwnerUserId(1L);
            o.setTransformStatus(false);
        });
        clueMapper.insert(clue);

        // 调用
        clueService.deleteClue(clue.getId());

        // 校验
        assertNull(clueMapper.selectById(clue.getId()));
    }

    @Test
    public void test_deleteClue_notExists() {
        assertThrows(Exception.class, () -> clueService.deleteClue(randomLongId()));
    }

    @Test
    public void test_getClue_success() {
        // mock 数据
        CrmClueDO clue = randomPojo(CrmClueDO.class);
        clueMapper.insert(clue);

        // 调用并校验
        CrmClueDO result = clueService.getClue(clue.getId());
        assertNotNull(result);
        assertEquals(clue.getId(), result.getId());
    }

    @Test
    public void test_getClue_notExists() {
        assertNull(clueService.getClue(randomLongId()));
    }

    @Test
    public void test_getCluePage_success() {
        // mock 数据
        CrmClueDO clue = randomPojo(CrmClueDO.class, o -> {
            o.setName("搜索关键词");
            o.setOwnerUserId(1L);
        });
        clueMapper.insert(clue);

        // 准备参数
        CrmCluePageReqVO reqVO = new CrmCluePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        // 调用并校验
        var pageResult = clueService.getCluePage(reqVO, 1L);
        assertNotNull(pageResult);
    }

}
