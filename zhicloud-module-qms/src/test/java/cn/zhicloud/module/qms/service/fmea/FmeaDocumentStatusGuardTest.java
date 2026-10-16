package cn.zhicloud.module.qms.service.fmea;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.fmea.vo.FmeaDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.fmea.FmeaDocumentDO;
import cn.zhicloud.module.qms.dal.mysql.fmea.FmeaDocumentMapper;
import cn.zhicloud.module.qms.enums.qms.FmeaStatusEnum;
import cn.zhicloud.module.qms.enums.qms.FmeaTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：FMEA 文档建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 APPROVED(30)，造出一份「未评审、无失效模式条目、
 * 无 RPN」却已批准的 FMEA——FMEA 批准是控制计划与作业指导书的输入，
 * 凭空批准等于让下游文件失去依据。
 *
 * @author 智云
 */
@Import(FmeaDocumentServiceImpl.class)
public class FmeaDocumentStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private FmeaDocumentServiceImpl fmeaDocumentService;

    @Resource
    private FmeaDocumentMapper fmeaDocumentMapper;

    @Test
    public void test_createFmeaDocument_ignoresSuppliedApprovedStatus() {
        // 攻击形态：建单即批准（跳过评审）（§12.235.5）
        FmeaDocumentSaveReqVO req = new FmeaDocumentSaveReqVO();
        req.setFmeaNo("FMEA-BYPASS-APPROVED-001");
        req.setFmeaType(FmeaTypeEnum.DFMEA.getFmeaType());
        req.setStatus(FmeaStatusEnum.APPROVED.getStatus());

        Long id = fmeaDocumentService.createFmeaDocument(req);

        FmeaDocumentDO saved = fmeaDocumentMapper.selectById(id);
        assertEquals(FmeaStatusEnum.DRAFT.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createFmeaDocument_ignoresSuppliedReviewedStatus() {
        // 对称检查：任何非 DRAFT 状态都不生效（§12.235.5）
        FmeaDocumentSaveReqVO req = new FmeaDocumentSaveReqVO();
        req.setFmeaNo("FMEA-BYPASS-REVIEWED-001");
        req.setFmeaType(FmeaTypeEnum.PFMEA.getFmeaType());
        req.setStatus(FmeaStatusEnum.REVIEWED.getStatus());

        Long id = fmeaDocumentService.createFmeaDocument(req);

        FmeaDocumentDO saved = fmeaDocumentMapper.selectById(id);
        assertEquals(FmeaStatusEnum.DRAFT.getStatus(), saved.getStatus());
    }
}
