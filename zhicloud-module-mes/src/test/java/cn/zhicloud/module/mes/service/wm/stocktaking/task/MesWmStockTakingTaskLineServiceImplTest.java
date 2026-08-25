package cn.zhicloud.module.mes.service.wm.stocktaking.task;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.wm.materialstock.vo.MesWmMaterialStockListReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.stocktaking.task.vo.line.MesWmStockTakingTaskLinePageReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.stocktaking.task.vo.line.MesWmStockTakingTaskLineSaveReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.stocktaking.task.vo.result.MesWmStockTakingTaskResultSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.stocktaking.plan.MesWmStockTakingPlanParamDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.stocktaking.task.MesWmStockTakingTaskDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.stocktaking.task.MesWmStockTakingTaskLineDO;
import cn.zhicloud.module.mes.dal.mysql.wm.stocktaking.task.MesWmStockTakingTaskLineMapper;
import cn.zhicloud.module.mes.enums.wm.MesWmStockTakingPlanParamTypeEnum;
import cn.zhicloud.module.mes.enums.wm.MesWmStockTakingTaskLineStatusEnum;
import cn.zhicloud.module.mes.enums.wm.MesWmStockTakingTypeEnum;
import cn.zhicloud.module.mes.service.wm.materialstock.MesWmMaterialStockService;
import cn.zhicloud.module.mes.service.wm.stocktaking.plan.MesWmStockTakingPlanParamService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.WM_STOCK_TAKING_TASK_LINE_NOT_EXISTS;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.WM_STOCK_TAKING_TASK_NO_STOCK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link MesWmStockTakingTaskLineServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmStockTakingTaskLineServiceImpl.class)
public class MesWmStockTakingTaskLineServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmStockTakingTaskLineMapper stockTakingTaskLineMapper;
    @MockitoBean
    private MesWmStockTakingPlanParamService stockTakingPlanParamService;
    @MockitoBean
    private MesWmMaterialStockService materialStockService;
    @MockitoBean
    private MesWmStockTakingTaskService stockTakingTaskService;

    @Resource
    private MesWmStockTakingTaskLineServiceImpl stockTakingTaskLineService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmStockTakingTaskLineDO line = inv.getArgument(0);
            line.setId(line.getId() == null ? 100L : line.getId());
            return 1;
        }).when(stockTakingTaskLineMapper).insert(any(MesWmStockTakingTaskLineDO.class));
        when(stockTakingTaskLineMapper.updateById(any(MesWmStockTakingTaskLineDO.class))).thenReturn(1);
        when(stockTakingTaskLineMapper.deleteById(anyLong())).thenReturn(1);
        when(stockTakingTaskLineMapper.insertBatch(anyList())).thenReturn(true);
    }

    private MesWmStockTakingTaskDO buildTask(Integer type) {
        return MesWmStockTakingTaskDO.builder().id(1L).code("T001").name("盘点任务")
                .planId(2L).type(type).build();
    }

    private List<MesWmStockTakingPlanParamDO> buildParams() {
        return Arrays.asList(
                MesWmStockTakingPlanParamDO.builder().id(1L).planId(2L)
                        .type(MesWmStockTakingPlanParamTypeEnum.WAREHOUSE.getType()).valueId(10L).build(),
                MesWmStockTakingPlanParamDO.builder().id(2L).planId(2L)
                        .type(MesWmStockTakingPlanParamTypeEnum.LOCATION.getType()).valueId(20L).build(),
                MesWmStockTakingPlanParamDO.builder().id(3L).planId(2L)
                        .type(MesWmStockTakingPlanParamTypeEnum.AREA.getType()).valueId(null).build());
    }

    private List<MesWmMaterialStockDO> buildStocks() {
        return Arrays.asList(MesWmMaterialStockDO.builder().id(5L).itemId(300L).batchId(400L)
                .quantity(new BigDecimal("10")).warehouseId(10L).locationId(20L).areaId(30L).build());
    }

    private MesWmStockTakingTaskLineDO buildLine(BigDecimal quantity) {
        return MesWmStockTakingTaskLineDO.builder().id(100L).taskId(1L).itemId(300L).batchId(400L)
                .quantity(quantity).takingQuantity(BigDecimal.ZERO)
                .warehouseId(10L).locationId(20L).areaId(30L)
                .status(MesWmStockTakingTaskLineStatusEnum.LOSS.getStatus()).build();
    }

    private MesWmStockTakingTaskLineSaveReqVO buildSaveReq() {
        return new MesWmStockTakingTaskLineSaveReqVO().setTaskId(1L).setItemId(300L).setBatchId(400L)
                .setQuantity(new BigDecimal("10")).setWarehouseId(10L).setLocationId(20L).setAreaId(30L);
    }

    // ========== generateStockTakingLines ==========

    @Test
    public void testGenerateStockTakingLines_createStatic() {
        when(stockTakingPlanParamService.getStockTakingPlanParamListByPlanId(2L)).thenReturn(buildParams());
        when(materialStockService.getMaterialStockList(any(MesWmMaterialStockListReqVO.class)))
                .thenReturn(buildStocks());

        stockTakingTaskLineService.generateStockTakingLines(
                buildTask(MesWmStockTakingTypeEnum.STATIC.getType()), true);

        verify(stockTakingTaskLineMapper, never()).deleteByTaskId(anyLong());
        verify(stockTakingTaskLineMapper).insertBatch(anyList());
    }

    @Test
    public void testGenerateStockTakingLines_regenerate() {
        when(stockTakingPlanParamService.getStockTakingPlanParamListByPlanId(2L)).thenReturn(buildParams());
        when(materialStockService.getMaterialStockList(any(MesWmMaterialStockListReqVO.class)))
                .thenReturn(buildStocks());

        stockTakingTaskLineService.generateStockTakingLines(
                buildTask(MesWmStockTakingTypeEnum.STATIC.getType()), false);

        verify(stockTakingTaskLineMapper).deleteByTaskId(1L);
        verify(stockTakingTaskLineMapper).insertBatch(anyList());
    }

    @Test
    public void testGenerateStockTakingLines_dynamic() {
        MesWmStockTakingTaskDO task = buildTask(MesWmStockTakingTypeEnum.DYNAMIC.getType())
                .setStartTime(LocalDateTime.now().minusDays(1)).setEndTime(LocalDateTime.now());
        when(stockTakingPlanParamService.getStockTakingPlanParamListByPlanId(2L)).thenReturn(buildParams());
        when(materialStockService.getMaterialStockList(any(MesWmMaterialStockListReqVO.class)))
                .thenReturn(buildStocks());

        stockTakingTaskLineService.generateStockTakingLines(task, true);

        verify(stockTakingTaskLineMapper).insertBatch(anyList());
    }

    @Test
    public void testGenerateStockTakingLines_dynamicStartTimeNull() {
        MesWmStockTakingTaskDO task = buildTask(MesWmStockTakingTypeEnum.DYNAMIC.getType());
        when(stockTakingPlanParamService.getStockTakingPlanParamListByPlanId(2L)).thenReturn(buildParams());

        assertThrows(IllegalArgumentException.class,
                () -> stockTakingTaskLineService.generateStockTakingLines(task, true));
    }

    @Test
    public void testGenerateStockTakingLines_paramsEmpty() {
        when(stockTakingPlanParamService.getStockTakingPlanParamListByPlanId(2L))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> stockTakingTaskLineService.generateStockTakingLines(
                buildTask(MesWmStockTakingTypeEnum.STATIC.getType()), true));
    }

    @Test
    public void testGenerateStockTakingLines_noStock() {
        when(stockTakingPlanParamService.getStockTakingPlanParamListByPlanId(2L)).thenReturn(buildParams());
        when(materialStockService.getMaterialStockList(any(MesWmMaterialStockListReqVO.class)))
                .thenReturn(Collections.emptyList());

        assertServiceException(() -> stockTakingTaskLineService.generateStockTakingLines(
                buildTask(MesWmStockTakingTypeEnum.STATIC.getType()), true), WM_STOCK_TAKING_TASK_NO_STOCK);
    }

    // ========== create / update / delete ==========

    @Test
    public void testCreateStockTakingTaskLine_success() {
        Long id = stockTakingTaskLineService.createStockTakingTaskLine(buildSaveReq());
        assertEquals(100L, id);
        verify(stockTakingTaskService).validateStockTakingTaskExistsAndPrepare(1L);
        verify(stockTakingTaskLineMapper).insert(any(MesWmStockTakingTaskLineDO.class));
    }

    @Test
    public void testCreateStockTakingTaskLine_defaultStatusLoss() {
        ArgumentCaptor<MesWmStockTakingTaskLineDO> captor =
                ArgumentCaptor.forClass(MesWmStockTakingTaskLineDO.class);
        stockTakingTaskLineService.createStockTakingTaskLine(buildSaveReq());
        verify(stockTakingTaskLineMapper).insert(captor.capture());
        assertEquals(MesWmStockTakingTaskLineStatusEnum.LOSS.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testUpdateStockTakingTaskLine_success() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(buildLine(new BigDecimal("10")));
        stockTakingTaskLineService.updateStockTakingTaskLine(buildSaveReq().setId(100L));
        verify(stockTakingTaskService).validateStockTakingTaskExistsAndPrepare(1L);
        verify(stockTakingTaskLineMapper).updateById(any(MesWmStockTakingTaskLineDO.class));
    }

    @Test
    public void testUpdateStockTakingTaskLine_notExists() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> stockTakingTaskLineService.updateStockTakingTaskLine(buildSaveReq().setId(100L)),
                WM_STOCK_TAKING_TASK_LINE_NOT_EXISTS);
    }

    @Test
    public void testDeleteStockTakingTaskLine_success() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(buildLine(new BigDecimal("10")));
        stockTakingTaskLineService.deleteStockTakingTaskLine(100L);
        verify(stockTakingTaskService).validateStockTakingTaskExistsAndPrepare(1L);
        verify(stockTakingTaskLineMapper).deleteById(100L);
    }

    @Test
    public void testDeleteStockTakingTaskLine_notExists() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> stockTakingTaskLineService.deleteStockTakingTaskLine(100L),
                WM_STOCK_TAKING_TASK_LINE_NOT_EXISTS);
    }

    // ========== get / page / list ==========

    @Test
    public void testGetStockTakingTaskLine() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(buildLine(new BigDecimal("10")));
        assertNotNull(stockTakingTaskLineService.getStockTakingTaskLine(100L));
    }

    @Test
    public void testGetStockTakingTaskLine_null() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(null);
        assertNull(stockTakingTaskLineService.getStockTakingTaskLine(100L));
    }

    @Test
    public void testGetStockTakingTaskLinePage() {
        PageResult<MesWmStockTakingTaskLineDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(stockTakingTaskLineMapper.selectPage(any(MesWmStockTakingTaskLinePageReqVO.class))).thenReturn(page);
        assertEquals(0L, stockTakingTaskLineService.getStockTakingTaskLinePage(
                new MesWmStockTakingTaskLinePageReqVO()).getTotal());
    }

    @Test
    public void testGetStockTakingTaskLineListByTaskId() {
        when(stockTakingTaskLineMapper.selectListByTaskId(1L))
                .thenReturn(Arrays.asList(buildLine(new BigDecimal("10"))));
        assertEquals(1, stockTakingTaskLineService.getStockTakingTaskLineListByTaskId(1L).size());
    }

    @Test
    public void testDeleteStockTakingTaskLineByTaskId() {
        stockTakingTaskLineService.deleteStockTakingTaskLineByTaskId(1L);
        verify(stockTakingTaskLineMapper).deleteByTaskId(1L);
    }

    @Test
    public void testGetStockTakingTaskLineByTaskIdAndItemIdAndAreaId() {
        when(stockTakingTaskLineMapper.selectByTaskIdAndItemIdAndAreaId(1L, 300L, 30L))
                .thenReturn(buildLine(new BigDecimal("10")));
        assertNotNull(stockTakingTaskLineService.getStockTakingTaskLine(1L, 300L, 30L));
    }

    // ========== updateStockTakingTaskLineTakingQuantity ==========

    @Test
    public void testUpdateStockTakingTaskLineTakingQuantity_gain() {
        MesWmStockTakingTaskLineDO line = buildLine(new BigDecimal("10"));
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(line);
        stockTakingTaskLineService.updateStockTakingTaskLineTakingQuantity(100L, new BigDecimal("20"));
        assertEquals(MesWmStockTakingTaskLineStatusEnum.GAIN.getStatus(), line.getStatus());
        assertEquals(0, new BigDecimal("20").compareTo(line.getTakingQuantity()));
        verify(stockTakingTaskLineMapper).updateById(any(MesWmStockTakingTaskLineDO.class));
    }

    @Test
    public void testUpdateStockTakingTaskLineTakingQuantity_loss() {
        MesWmStockTakingTaskLineDO line = buildLine(new BigDecimal("10"));
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(line);
        stockTakingTaskLineService.updateStockTakingTaskLineTakingQuantity(100L, new BigDecimal("5"));
        assertEquals(MesWmStockTakingTaskLineStatusEnum.LOSS.getStatus(), line.getStatus());
    }

    @Test
    public void testUpdateStockTakingTaskLineTakingQuantity_normal() {
        MesWmStockTakingTaskLineDO line = buildLine(new BigDecimal("10"));
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(line);
        stockTakingTaskLineService.updateStockTakingTaskLineTakingQuantity(100L, new BigDecimal("10"));
        assertEquals(MesWmStockTakingTaskLineStatusEnum.NORMAL.getStatus(), line.getStatus());
    }

    @Test
    public void testUpdateStockTakingTaskLineTakingQuantity_notExists() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> stockTakingTaskLineService.updateStockTakingTaskLineTakingQuantity(
                100L, new BigDecimal("10")), WM_STOCK_TAKING_TASK_LINE_NOT_EXISTS);
    }

    @Test
    public void testUpdateStockTakingTaskLineTakingQuantity_takingQuantityNull() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(buildLine(new BigDecimal("10")));
        assertThrows(IllegalArgumentException.class,
                () -> stockTakingTaskLineService.updateStockTakingTaskLineTakingQuantity(100L, null));
    }

    // ========== createStockTakingTaskLine（盘点结果） ==========

    @Test
    public void testCreateStockTakingTaskLineByResult_gain() {
        MesWmStockTakingTaskResultSaveReqVO reqVO = new MesWmStockTakingTaskResultSaveReqVO()
                .setTaskId(1L).setItemId(300L).setBatchId(400L)
                .setWarehouseId(10L).setLocationId(20L).setAreaId(30L)
                .setTakingQuantity(new BigDecimal("8"));

        ArgumentCaptor<MesWmStockTakingTaskLineDO> captor =
                ArgumentCaptor.forClass(MesWmStockTakingTaskLineDO.class);
        Long id = stockTakingTaskLineService.createStockTakingTaskLine(reqVO);

        assertEquals(100L, id);
        verify(stockTakingTaskLineMapper).insert(captor.capture());
        assertEquals(MesWmStockTakingTaskLineStatusEnum.GAIN.getStatus(), captor.getValue().getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getQuantity()));
    }

    @Test
    public void testCreateStockTakingTaskLineByResult_normal() {
        MesWmStockTakingTaskResultSaveReqVO reqVO = new MesWmStockTakingTaskResultSaveReqVO()
                .setTaskId(1L).setItemId(300L)
                .setWarehouseId(10L).setLocationId(20L).setAreaId(30L)
                .setTakingQuantity(BigDecimal.ZERO);

        ArgumentCaptor<MesWmStockTakingTaskLineDO> captor =
                ArgumentCaptor.forClass(MesWmStockTakingTaskLineDO.class);
        stockTakingTaskLineService.createStockTakingTaskLine(reqVO);

        verify(stockTakingTaskLineMapper).insert(captor.capture());
        assertEquals(MesWmStockTakingTaskLineStatusEnum.NORMAL.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void testCreateStockTakingTaskLineByResult_takingQuantityNull() {
        MesWmStockTakingTaskResultSaveReqVO reqVO = new MesWmStockTakingTaskResultSaveReqVO()
                .setTaskId(1L).setItemId(300L)
                .setWarehouseId(10L).setLocationId(20L).setAreaId(30L);
        assertThrows(IllegalArgumentException.class,
                () -> stockTakingTaskLineService.createStockTakingTaskLine(reqVO));
    }

    // ========== validateStockTakingTaskLineExists ==========

    @Test
    public void testValidateStockTakingTaskLineExists_exists() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(buildLine(new BigDecimal("10")));
        assertNotNull(stockTakingTaskLineService.validateStockTakingTaskLineExists(100L));
    }

    @Test
    public void testValidateStockTakingTaskLineExists_notExists() {
        when(stockTakingTaskLineMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> stockTakingTaskLineService.validateStockTakingTaskLineExists(100L),
                WM_STOCK_TAKING_TASK_LINE_NOT_EXISTS);
    }

}
