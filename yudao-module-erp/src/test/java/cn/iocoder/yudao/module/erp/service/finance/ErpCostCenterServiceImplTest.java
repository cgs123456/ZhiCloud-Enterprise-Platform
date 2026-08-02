package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCostCenterDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpCostCenterMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link ErpCostCenterServiceImpl} 的单元测试（Phase 4 核心域补测）
 *
 * @author ZhiCloud 平台加固
 */
@ExtendWith(MockitoExtension.class)
public class ErpCostCenterServiceImplTest {

    @Mock
    private ErpCostCenterMapper costCenterMapper;
    @InjectMocks
    private ErpCostCenterServiceImpl costCenterService;

    @Test
    public void test_getCostCenter_success() {
        ErpCostCenterDO center = new ErpCostCenterDO().setId(1L).setCode("CC001");
        when(costCenterMapper.selectById(1L)).thenReturn(center);

        ErpCostCenterDO result = costCenterService.getCostCenter(1L);
        assertNotNull(result);
        assertEquals("CC001", result.getCode());
    }

    @Test
    public void test_getCostCenterList_success() {
        List<ErpCostCenterDO> list = List.of(new ErpCostCenterDO().setId(1L), new ErpCostCenterDO().setId(2L));
        when(costCenterMapper.selectList(null)).thenReturn(list);

        List<ErpCostCenterDO> result = costCenterService.getCostCenterList();
        assertEquals(2, result.size());
    }

    @Test
    public void test_getCostCenterPage_success() {
        when(costCenterMapper.selectPage(any())).thenReturn(PageResult.empty());

        PageResult<ErpCostCenterDO> page = costCenterService.getCostCenterPage(new ErpCostCenterPageReqVO());
        assertNotNull(page);
    }

    @Test
    public void test_createCostCenter_success() {
        // 编码不重复、无父级
        when(costCenterMapper.selectByCode(any())).thenReturn(null);
        // 模拟 MyBatis-Plus 主键回填
        doAnswer(invocation -> {
            ErpCostCenterDO arg = invocation.getArgument(0);
            arg.setId(99L);
            return null;
        }).when(costCenterMapper).insert(any(ErpCostCenterDO.class));

        ErpCostCenterSaveReqVO reqVO = new ErpCostCenterSaveReqVO();
        reqVO.setCode("CC001");
        reqVO.setName("成本中心A");

        Long id = costCenterService.createCostCenter(reqVO);
        assertEquals(99L, id);
        verify(costCenterMapper).insert(any(ErpCostCenterDO.class));
    }

    @Test
    public void test_deleteCostCenter_success() {
        ErpCostCenterDO center = new ErpCostCenterDO().setId(1L).setCode("CC001");
        when(costCenterMapper.selectById(1L)).thenReturn(center);
        when(costCenterMapper.selectListByParentId(1L)).thenReturn(Collections.emptyList());

        costCenterService.deleteCostCenter(1L);

        verify(costCenterMapper).deleteById(1L);
    }

    @Test
    public void test_deleteCostCenter_hasChildren() {
        ErpCostCenterDO center = new ErpCostCenterDO().setId(1L).setCode("CC001");
        when(costCenterMapper.selectById(1L)).thenReturn(center);
        when(costCenterMapper.selectListByParentId(1L)).thenReturn(List.of(new ErpCostCenterDO().setId(2L)));

        assertThrows(Exception.class, () -> costCenterService.deleteCostCenter(1L));
    }

}
