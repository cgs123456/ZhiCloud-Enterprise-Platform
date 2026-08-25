package cn.zhicloud.module.mes.service.md.client;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.md.client.vo.MesMdClientImportExcelVO;
import cn.zhicloud.module.mes.controller.admin.md.client.vo.MesMdClientImportRespVO;
import cn.zhicloud.module.mes.controller.admin.md.client.vo.MesMdClientPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.client.vo.MesMdClientSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.client.MesMdClientDO;
import cn.zhicloud.module.mes.dal.mysql.md.client.MesMdClientMapper;
import cn.zhicloud.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.zhicloud.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link MesMdClientServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesMdClientServiceImpl.class)
public class MesMdClientServiceImplTest extends BaseDbUnitTest {

    @MockitoBean
    private MesMdClientMapper clientMapper;
    @MockitoBean
    private MesWmBarcodeService barcodeService;

    @Resource
    private MesMdClientServiceImpl clientService;

    @BeforeEach
    public void setUp() {
        // insert 时回填主键
        doAnswer(inv -> {
            MesMdClientDO client = inv.getArgument(0);
            client.setId(client.getId() == null ? 100L : client.getId());
            return 1;
        }).when(clientMapper).insert(any(MesMdClientDO.class));
        when(clientMapper.updateById(any(MesMdClientDO.class))).thenReturn(1);
        when(clientMapper.deleteById(anyLong())).thenReturn(1);
    }

    private MesMdClientDO buildClient() {
        return new MesMdClientDO().setId(100L).setCode("C001").setName("客户A").setNickname("A")
                .setType(1).setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private MesMdClientSaveReqVO buildSaveReq() {
        return new MesMdClientSaveReqVO().setCode("C001").setName("客户A").setNickname("A")
                .setType(1).setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private MesMdClientImportExcelVO buildImportVO() {
        return MesMdClientImportExcelVO.builder().code("C001").name("客户A").nickname("A").type(1).build();
    }

    // ========== createClient ==========

    @Test
    public void testCreateClient_success() {
        Long id = clientService.createClient(buildSaveReq());
        assertEquals(100L, id);
        verify(clientMapper).insert(any(MesMdClientDO.class));
        verify(barcodeService).autoGenerateBarcode(eq(BarcodeBizTypeEnum.CLIENT.getValue()),
                eq(100L), eq("C001"), eq("客户A"));
    }

    @Test
    public void testCreateClient_codeDuplicate() {
        when(clientMapper.selectByCode("C001")).thenReturn(buildClient());
        assertServiceException(() -> clientService.createClient(buildSaveReq()), MD_CLIENT_CODE_DUPLICATE);
        verify(clientMapper, never()).insert(any(MesMdClientDO.class));
    }

    @Test
    public void testCreateClient_nameDuplicate() {
        when(clientMapper.selectByName("客户A")).thenReturn(buildClient());
        assertServiceException(() -> clientService.createClient(buildSaveReq()), MD_CLIENT_NAME_DUPLICATE);
    }

    @Test
    public void testCreateClient_nicknameDuplicate() {
        when(clientMapper.selectByNickname("A")).thenReturn(buildClient());
        assertServiceException(() -> clientService.createClient(buildSaveReq()), MD_CLIENT_NICKNAME_DUPLICATE);
    }

    @Test
    public void testCreateClient_blankNicknameSkipValidate() {
        MesMdClientSaveReqVO reqVO = buildSaveReq().setNickname("");
        Long id = clientService.createClient(reqVO);
        assertEquals(100L, id);
        verify(clientMapper, never()).selectByNickname(anyString());
    }

    // ========== updateClient ==========

    @Test
    public void testUpdateClient_success() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        clientService.updateClient(buildSaveReq().setId(100L).setName("客户B"));
        verify(clientMapper).updateById(any(MesMdClientDO.class));
    }

    @Test
    public void testUpdateClient_notExists() {
        when(clientMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> clientService.updateClient(buildSaveReq().setId(100L)), MD_CLIENT_NOT_EXISTS);
        verify(clientMapper, never()).updateById(any(MesMdClientDO.class));
    }

    @Test
    public void testUpdateClient_codeDuplicateOnOther() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        when(clientMapper.selectByCode("C001")).thenReturn(buildClient().setId(101L));
        assertServiceException(() -> clientService.updateClient(buildSaveReq().setId(100L)), MD_CLIENT_CODE_DUPLICATE);
    }

    @Test
    public void testUpdateClient_nameDuplicateOnOther() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        when(clientMapper.selectByName("客户A")).thenReturn(buildClient().setId(101L));
        assertServiceException(() -> clientService.updateClient(buildSaveReq().setId(100L)), MD_CLIENT_NAME_DUPLICATE);
    }

    @Test
    public void testUpdateClient_nicknameDuplicateOnOther() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        when(clientMapper.selectByNickname("A")).thenReturn(buildClient().setId(101L));
        assertServiceException(() -> clientService.updateClient(buildSaveReq().setId(100L)),
                MD_CLIENT_NICKNAME_DUPLICATE);
    }

    @Test
    public void testUpdateClient_selfNotDuplicate() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        when(clientMapper.selectByCode("C001")).thenReturn(buildClient());
        when(clientMapper.selectByName("客户A")).thenReturn(buildClient());
        when(clientMapper.selectByNickname("A")).thenReturn(buildClient());
        clientService.updateClient(buildSaveReq().setId(100L));
        verify(clientMapper).updateById(any(MesMdClientDO.class));
    }

    // ========== deleteClient ==========

    @Test
    public void testDeleteClient_success() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        clientService.deleteClient(100L);
        verify(clientMapper).deleteById(100L);
    }

    @Test
    public void testDeleteClient_notExists() {
        when(clientMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> clientService.deleteClient(100L), MD_CLIENT_NOT_EXISTS);
        verify(clientMapper, never()).deleteById(anyLong());
    }

    // ========== validate ==========

    @Test
    public void testValidateClientExists_exists() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        Assertions.assertDoesNotThrow(() -> clientService.validateClientExists(100L));
    }

    @Test
    public void testValidateClientExists_notExists() {
        when(clientMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> clientService.validateClientExists(100L), MD_CLIENT_NOT_EXISTS);
    }

    @Test
    public void testValidateClientExistsAndEnable_enabled() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        Assertions.assertDoesNotThrow(() -> clientService.validateClientExistsAndEnable(100L));
    }

    @Test
    public void testValidateClientExistsAndEnable_notExists() {
        when(clientMapper.selectById(100L)).thenReturn(null);
        assertServiceException(() -> clientService.validateClientExistsAndEnable(100L), MD_CLIENT_NOT_EXISTS);
    }

    @Test
    public void testValidateClientExistsAndEnable_disabled() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient().setStatus(CommonStatusEnum.DISABLE.getStatus()));
        assertServiceException(() -> clientService.validateClientExistsAndEnable(100L), MD_CLIENT_IS_DISABLE);
    }

    // ========== get / list / page ==========

    @Test
    public void testGetClient_exists() {
        when(clientMapper.selectById(100L)).thenReturn(buildClient());
        assertNotNull(clientService.getClient(100L));
    }

    @Test
    public void testGetClient_notExists() {
        when(clientMapper.selectById(100L)).thenReturn(null);
        assertNull(clientService.getClient(100L));
    }

    @Test
    public void testGetClientPage() {
        PageResult<MesMdClientDO> page = new PageResult<>(Collections.singletonList(buildClient()), 1L);
        when(clientMapper.selectPage(any(MesMdClientPageReqVO.class))).thenReturn(page);
        PageResult<MesMdClientDO> result = clientService.getClientPage(new MesMdClientPageReqVO());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    @Test
    public void testGetClientList_empty() {
        assertTrue(clientService.getClientList(Collections.emptyList()).isEmpty());
        verify(clientMapper, never()).selectByIds(any());
    }

    @Test
    public void testGetClientList_nonEmpty() {
        when(clientMapper.selectByIds(any())).thenReturn(Arrays.asList(buildClient()));
        List<MesMdClientDO> list = clientService.getClientList(Arrays.asList(100L));
        assertEquals(1, list.size());
    }

    // ========== importClientList ==========

    @Test
    public void testImportClientList_empty() {
        assertServiceException(() -> clientService.importClientList(Collections.emptyList(), true),
                MD_CLIENT_IMPORT_LIST_IS_EMPTY);
    }

    @Test
    public void testImportClientList_create() {
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getCreateCodes().size());
        assertTrue(resp.getFailureCodes().isEmpty());
        verify(clientMapper).insert(any(MesMdClientDO.class));
        verify(barcodeService).autoGenerateBarcode(eq(BarcodeBizTypeEnum.CLIENT.getValue()),
                eq(100L), eq("C001"), eq("客户A"));
    }

    @Test
    public void testImportClientList_blankCode() {
        MesMdClientImportExcelVO vo = MesMdClientImportExcelVO.builder().name("客户A").type(1).build();
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("客户编码不能为空", resp.getFailureCodes().get("第 1 行"));
    }

    @Test
    public void testImportClientList_blankName() {
        MesMdClientImportExcelVO vo = MesMdClientImportExcelVO.builder().code("C001").type(1).build();
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("客户名称不能为空", resp.getFailureCodes().get("C001"));
    }

    @Test
    public void testImportClientList_typeNull() {
        MesMdClientImportExcelVO vo = MesMdClientImportExcelVO.builder().code("C001").name("客户A").build();
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(vo), true);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("客户类型不能为空", resp.getFailureCodes().get("C001"));
    }

    @Test
    public void testImportClientList_nameDuplicate() {
        when(clientMapper.selectByName("客户A")).thenReturn(buildClient());
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getFailureCodes().size());
        verify(clientMapper, never()).insert(any(MesMdClientDO.class));
    }

    @Test
    public void testImportClientList_nicknameDuplicate() {
        when(clientMapper.selectByNickname("A")).thenReturn(buildClient());
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getFailureCodes().size());
        verify(clientMapper, never()).insert(any(MesMdClientDO.class));
    }

    @Test
    public void testImportClientList_update() {
        when(clientMapper.selectByCode("C001")).thenReturn(buildClient());
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getUpdateCodes().size());
        verify(clientMapper).updateById(any(MesMdClientDO.class));
    }

    @Test
    public void testImportClientList_updateNameDuplicate() {
        when(clientMapper.selectByCode("C001")).thenReturn(buildClient());
        when(clientMapper.selectByName("客户A")).thenReturn(buildClient().setId(101L));
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(buildImportVO()), true);
        assertEquals(1, resp.getFailureCodes().size());
        verify(clientMapper, never()).updateById(any(MesMdClientDO.class));
    }

    @Test
    public void testImportClientList_noUpdateSupport() {
        when(clientMapper.selectByCode("C001")).thenReturn(buildClient());
        MesMdClientImportRespVO resp = clientService.importClientList(Arrays.asList(buildImportVO()), false);
        assertEquals(1, resp.getFailureCodes().size());
        assertEquals("客户编码已存在", resp.getFailureCodes().get("C001"));
        verify(clientMapper, never()).updateById(any(MesMdClientDO.class));
    }

}
