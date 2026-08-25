package cn.zhicloud.module.mes.service.wm.packages;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.wm.packages.vo.MesWmPackagePageReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.packages.vo.MesWmPackageSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.packages.MesWmPackageDO;
import cn.zhicloud.module.mes.dal.mysql.wm.packages.MesWmPackageMapper;
import cn.zhicloud.module.mes.enums.wm.MesWmPackageStatusEnum;
import cn.zhicloud.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

/**
 * {@link MesWmPackageServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesWmPackageServiceImpl.class)
public class MesWmPackageServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesWmPackageMapper packageMapper;
    @MockitoBean
    private MesWmPackageLineService packageLineService;
    @MockitoBean
    private MesWmBarcodeService barcodeService;

    @Resource
    private MesWmPackageServiceImpl packageService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesWmPackageDO packageDO = inv.getArgument(0);
            packageDO.setId(packageDO.getId() == null ? 100L : packageDO.getId());
            return 1;
        }).when(packageMapper).insert(any(MesWmPackageDO.class));
        when(packageMapper.updateById(any(MesWmPackageDO.class))).thenReturn(1);
        when(packageMapper.deleteById(anyLong())).thenReturn(1);
    }

    private MesWmPackageDO buildDraftPackage(Long id) {
        return new MesWmPackageDO().setId(id).setCode("PKG" + id)
                .setParentId(MesWmPackageDO.PARENT_ID_ROOT)
                .setPackageDate(LocalDateTime.now())
                .setStatus(MesWmPackageStatusEnum.PREPARE.getStatus());
    }

    private MesWmPackageDO buildFinishedPackage(Long id) {
        return buildDraftPackage(id).setStatus(MesWmPackageStatusEnum.FINISHED.getStatus());
    }

    private MesWmPackageSaveReqVO buildSaveReq() {
        MesWmPackageSaveReqVO reqVO = new MesWmPackageSaveReqVO();
        reqVO.setCode("PKG001");
        reqVO.setPackageDate(LocalDateTime.now());
        reqVO.setSalesOrderCode("SO001");
        return reqVO;
    }

    // ========== createPackage ==========

    @Test
    public void testCreatePackage_success() {
        Long id = packageService.createPackage(buildSaveReq());
        assertEquals(100L, id);
        ArgumentCaptor<MesWmPackageDO> captor = ArgumentCaptor.forClass(MesWmPackageDO.class);
        verify(packageMapper).insert(captor.capture());
        assertEquals(MesWmPackageStatusEnum.PREPARE.getStatus(), captor.getValue().getStatus());
        assertEquals(MesWmPackageDO.PARENT_ID_ROOT, captor.getValue().getParentId());
        verify(barcodeService).autoGenerateBarcode(any(), eq(100L), eq("PKG001"), isNull());
    }

    @Test
    public void testCreatePackage_codeDuplicate() {
        when(packageMapper.selectByCode("PKG001")).thenReturn(buildDraftPackage(1L));
        assertServiceException(() -> packageService.createPackage(buildSaveReq()), WM_PACKAGE_CODE_DUPLICATE);
        verify(packageMapper, never()).insert(any(MesWmPackageDO.class));
    }

    // ========== updatePackage ==========

    @Test
    public void testUpdatePackage_success() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        MesWmPackageSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        packageService.updatePackage(reqVO);
        verify(packageMapper).updateById(any(MesWmPackageDO.class));
    }

    @Test
    public void testUpdatePackage_notExists() {
        when(packageMapper.selectById(100L)).thenReturn(null);
        MesWmPackageSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> packageService.updatePackage(reqVO), WM_PACKAGE_NOT_EXISTS);
    }

    @Test
    public void testUpdatePackage_statusNotPrepare() {
        when(packageMapper.selectById(100L)).thenReturn(buildFinishedPackage(100L));
        MesWmPackageSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> packageService.updatePackage(reqVO), WM_PACKAGE_STATUS_NOT_PREPARE);
    }

    @Test
    public void testUpdatePackage_codeDuplicate() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        when(packageMapper.selectByCode("PKG001")).thenReturn(buildDraftPackage(200L));
        MesWmPackageSaveReqVO reqVO = buildSaveReq();
        reqVO.setId(100L);
        assertServiceException(() -> packageService.updatePackage(reqVO), WM_PACKAGE_CODE_DUPLICATE);
    }

    // ========== deletePackage ==========

    @Test
    public void testDeletePackage_success() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        when(packageMapper.selectListByParentId(100L)).thenReturn(Collections.emptyList());
        packageService.deletePackage(100L);
        verify(packageMapper).deleteById(100L);
        verify(packageLineService).deletePackageLineByPackageId(100L);
    }

    @Test
    public void testDeletePackage_notExists() {
        when(packageMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> packageService.deletePackage(100L), WM_PACKAGE_NOT_EXISTS);
    }

    @Test
    public void testDeletePackage_hasChildren() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        when(packageMapper.selectListByParentId(100L)).thenReturn(Arrays.asList(buildFinishedPackage(200L)));
        assertServiceException(() -> packageService.deletePackage(100L), WM_PACKAGE_HAS_CHILDREN);
        verify(packageMapper, never()).deleteById(anyLong());
    }

    // ========== get / page ==========

    @Test
    public void testGetPackage() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        assertNotNull(packageService.getPackage(100L));
    }

    @Test
    public void testGetPackage_notExists() {
        when(packageMapper.selectById(999L)).thenReturn(null);
        assertNull(packageService.getPackage(999L));
    }

    @Test
    public void testGetPackagePage() {
        PageResult<MesWmPackageDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(packageMapper.selectPage(any(MesWmPackagePageReqVO.class))).thenReturn(page);
        assertEquals(0, packageService.getPackagePage(new MesWmPackagePageReqVO()).getTotal());
    }

    // ========== finishPackage / validatePackageStatusDraft ==========

    @Test
    public void testFinishPackage_success() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        packageService.finishPackage(100L);
        verify(packageMapper).updateById(argThat((MesWmPackageDO packageDO) ->
                MesWmPackageStatusEnum.FINISHED.getStatus().equals(packageDO.getStatus())));
    }

    @Test
    public void testFinishPackage_statusNotPrepare() {
        when(packageMapper.selectById(100L)).thenReturn(buildFinishedPackage(100L));
        assertServiceException(() -> packageService.finishPackage(100L), WM_PACKAGE_STATUS_NOT_PREPARE);
    }

    @Test
    public void testValidatePackageStatusDraft_success() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        packageService.validatePackageStatusDraft(100L);
        verify(packageMapper).selectById(100L);
    }

    @Test
    public void testValidatePackageStatusDraft_notPrepare() {
        when(packageMapper.selectById(100L)).thenReturn(buildFinishedPackage(100L));
        assertServiceException(() -> packageService.validatePackageStatusDraft(100L), WM_PACKAGE_STATUS_NOT_PREPARE);
    }

    // ========== addChildPackage ==========

    @Test
    public void testAddChildPackage_success() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        when(packageMapper.selectById(200L)).thenReturn(buildFinishedPackage(200L));
        packageService.addChildPackage(100L, 200L);
        verify(packageMapper).updateById(argThat((MesWmPackageDO packageDO) ->
                Long.valueOf(200L).equals(packageDO.getId()) && Long.valueOf(100L).equals(packageDO.getParentId())));
    }

    @Test
    public void testAddChildPackage_parentSelf() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        assertServiceException(() -> packageService.addChildPackage(100L, 100L), WM_PACKAGE_PARENT_SELF);
    }

    @Test
    public void testAddChildPackage_childHasParent() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        when(packageMapper.selectById(200L)).thenReturn(buildFinishedPackage(200L).setParentId(50L));
        assertServiceException(() -> packageService.addChildPackage(100L, 200L), WM_PACKAGE_CHILD_HAS_PARENT);
    }

    @Test
    public void testAddChildPackage_childNotFinished() {
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        when(packageMapper.selectById(200L)).thenReturn(buildDraftPackage(200L));
        assertServiceException(() -> packageService.addChildPackage(100L, 200L), WM_PACKAGE_CHILD_NOT_FINISHED);
    }

    @Test
    public void testAddChildPackage_parentIsChild() {
        // 父箱 100 的父箱是 200，此时再把 200 挂到 100 下会形成环路
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L).setParentId(200L));
        when(packageMapper.selectById(200L)).thenReturn(buildFinishedPackage(200L));
        assertServiceException(() -> packageService.addChildPackage(100L, 200L), WM_PACKAGE_PARENT_IS_CHILD);
    }

    @Test
    public void testAddChildPackage_parentNotExists() {
        when(packageMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> packageService.addChildPackage(100L, 200L), WM_PACKAGE_NOT_EXISTS);
    }

    // ========== removeChildPackage ==========

    @Test
    public void testRemoveChildPackage_rootParent() {
        when(packageMapper.selectById(200L)).thenReturn(buildFinishedPackage(200L));
        packageService.removeChildPackage(200L);
        verify(packageMapper).updateById(argThat((MesWmPackageDO packageDO) ->
                MesWmPackageDO.PARENT_ID_ROOT.equals(packageDO.getParentId())));
    }

    @Test
    public void testRemoveChildPackage_withParent() {
        when(packageMapper.selectById(200L)).thenReturn(buildFinishedPackage(200L).setParentId(100L));
        when(packageMapper.selectById(100L)).thenReturn(buildDraftPackage(100L));
        packageService.removeChildPackage(200L);
        verify(packageMapper).updateById(any(MesWmPackageDO.class));
    }

    @Test
    public void testRemoveChildPackage_parentNotPrepare() {
        when(packageMapper.selectById(200L)).thenReturn(buildFinishedPackage(200L).setParentId(100L));
        when(packageMapper.selectById(100L)).thenReturn(buildFinishedPackage(100L));
        assertServiceException(() -> packageService.removeChildPackage(200L), WM_PACKAGE_STATUS_NOT_PREPARE);
    }

    @Test
    public void testRemoveChildPackage_notExists() {
        when(packageMapper.selectById(200L)).thenReturn(null);
        assertServiceException(() -> packageService.removeChildPackage(200L), WM_PACKAGE_NOT_EXISTS);
    }

    // ========== getPackageAndDescendantIds ==========

    @Test
    public void testGetPackageAndDescendantIds_noChildren() {
        when(packageMapper.selectListByParentIds(anyCollection())).thenReturn(Collections.emptyList());
        List<Long> ids = packageService.getPackageAndDescendantIds(100L);
        assertEquals(1, ids.size());
        assertEquals(100L, ids.get(0));
    }

    @Test
    public void testGetPackageAndDescendantIds_withChildren() {
        when(packageMapper.selectListByParentIds(anyCollection()))
                .thenReturn(Arrays.asList(buildFinishedPackage(200L), buildFinishedPackage(300L)))
                .thenReturn(Collections.emptyList());
        List<Long> ids = packageService.getPackageAndDescendantIds(100L);
        assertEquals(3, ids.size());
        assertTrue(ids.contains(200L));
        assertTrue(ids.contains(300L));
    }

}
