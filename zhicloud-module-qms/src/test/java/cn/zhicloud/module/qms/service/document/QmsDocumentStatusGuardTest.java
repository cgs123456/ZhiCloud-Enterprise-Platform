package cn.zhicloud.module.qms.service.document;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentDO;
import cn.zhicloud.module.qms.dal.mysql.document.QmsDocumentMapper;
import cn.zhicloud.module.qms.enums.document.QmsDocStatusEnum;
import cn.zhicloud.module.qms.enums.document.QmsDocTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：受控文档建单状态与审批留痕护栏测试。
 *
 * <p>攻击形态：建单直接传 PUBLISHED(30) 并自带 approverId，
 * 造出一份「无审批人、无电子签名、无审批日期」的已发布受控文件。
     * （approveDate 未出现在 SaveReqVO 中，结构上无法伪造，故只钉 approverId。）
 * approveDocument 上挂着 @ElectronicSignature，说明「发布」本应是受控动作，
 * 而旧实现只在 status 为 null 时补默认值，等于把受控动作开放成了一次 POST。
 *
 * @author 智云
 */
@Import(QmsDocumentServiceImpl.class)
public class QmsDocumentStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private QmsDocumentServiceImpl documentService;

    @Resource
    private QmsDocumentMapper documentMapper;

    @Test
    public void test_createDocument_ignoresSuppliedPublishedStatus() {
        // 攻击形态：建单即发布（跳过审批与电子签名），并伪造审批留痕（§12.235.5）
        QmsDocumentSaveReqVO req = new QmsDocumentSaveReqVO();
        req.setDocNo("QM-PR-BYPASS-001");
        req.setTitle("质量手册");
        req.setDocType(QmsDocTypeEnum.QUALITY_MANUAL.getDocType());
        req.setStatus(QmsDocStatusEnum.PUBLISHED.getStatus());
        req.setApproverId(randomLongId());

        Long id = documentService.createDocument(req);

        QmsDocumentDO saved = documentMapper.selectById(id);
        assertEquals(QmsDocStatusEnum.DRAFT.getStatus(), saved.getStatus());
        assertNull(saved.getApproverId(), "审批人只能由 approveDocument 写入，建单不得带入");
        assertNull(saved.getApproveDate(), "审批日期只能由 approveDocument 写入，建单不得带入");
    }

    @Test
    public void test_createDocument_ignoresSuppliedObsoleteStatus() {
        // 对称检查：任何非 DRAFT 状态都不生效（§12.235.5）
        QmsDocumentSaveReqVO req = new QmsDocumentSaveReqVO();
        req.setDocNo("QM-PR-BYPASS-002");
        req.setTitle("程序文件");
        req.setDocType(QmsDocTypeEnum.PROCEDURE.getDocType());
        req.setStatus(QmsDocStatusEnum.OBSOLETE.getStatus());

        Long id = documentService.createDocument(req);

        QmsDocumentDO saved = documentMapper.selectById(id);
        assertEquals(QmsDocStatusEnum.DRAFT.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createDocument_defaultStatusIsDraft() {
        QmsDocumentSaveReqVO req = new QmsDocumentSaveReqVO();
        req.setDocNo("QM-PR-DEFAULT-001");
        req.setTitle("作业指导书");
        req.setDocType(QmsDocTypeEnum.WORK_INSTRUCTION.getDocType());

        Long id = documentService.createDocument(req);

        QmsDocumentDO saved = documentMapper.selectById(id);
        assertEquals(QmsDocStatusEnum.DRAFT.getStatus(), saved.getStatus());
        assertNull(saved.getApproverId());
        assertNull(saved.getApproveDate());
    }
}
