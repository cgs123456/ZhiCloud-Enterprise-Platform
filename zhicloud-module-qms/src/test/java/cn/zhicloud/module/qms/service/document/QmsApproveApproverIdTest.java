package cn.zhicloud.module.qms.service.document;

import cn.zhicloud.framework.security.core.LoginUser;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentChangeRequestSaveReqVO;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentChangeRequestDO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentDO;
import cn.zhicloud.module.qms.dal.mysql.document.QmsDocumentChangeRequestMapper;
import cn.zhicloud.module.qms.dal.mysql.document.QmsDocumentMapper;
import cn.zhicloud.module.qms.enums.document.QmsChangeRequestStatusEnum;
import cn.zhicloud.module.qms.enums.document.QmsDocChangeTypeEnum;
import cn.zhicloud.module.qms.enums.document.QmsDocStatusEnum;
import cn.zhicloud.module.qms.enums.document.QmsDocTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * §12.245（A 组第 2 项）：approve 审批人落库测试（H2 真写）。
 *
 * <p>背景：§12.239.5 登记受控文档 approve 不写 approverId（列表「审批人 ID」列恒空）；
 * create 侧已被 §12.235.5 护栏钉死（直传强制置空），唯一合法写入者即 approve。
 * 变更申请 approve 同形缺口一并收口。
 *
 * <p>注：{@code @ElectronicSignature} 挂在 Controller 层，Service 单测直调无切面；
 * 真机路径 mock 身份无密码、过不了电子签名，故落库逻辑以本单测覆盖（见 §12.245.5）。
 *
 * @author 智云
 */
@Import({QmsDocumentServiceImpl.class, QmsDocumentChangeRequestServiceImpl.class})
public class QmsApproveApproverIdTest extends BaseDbUnitTest {

    private static final Long USER_ID = 900100L;

    @Resource
    private QmsDocumentServiceImpl documentService;
    @Resource
    private QmsDocumentChangeRequestServiceImpl changeRequestService;
    @Resource
    private QmsDocumentMapper documentMapper;
    @Resource
    private QmsDocumentChangeRequestMapper changeRequestMapper;

    @AfterEach
    public void cleanSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    private Long createDraftDocument(String docNo) {
        QmsDocumentSaveReqVO req = new QmsDocumentSaveReqVO();
        req.setDocNo(docNo);
        req.setTitle("审批人落库测试文档");
        req.setDocType(QmsDocTypeEnum.PROCEDURE.getDocType());
        return documentService.createDocument(req);
    }

    @Test
    public void test_approveDocument_writesApproverId() {
        Long id = createDraftDocument("QM-PR-APPROVER-001");
        documentService.submitDocument(id);
        loginAs(USER_ID);

        documentService.approveDocument(id, null);

        QmsDocumentDO saved = documentMapper.selectById(id);
        assertEquals(QmsDocStatusEnum.PUBLISHED.getStatus(), saved.getStatus());
        assertEquals(USER_ID, saved.getApproverId());
        assertNotNull(saved.getApproveDate());
    }

    @Test
    public void test_approveDocument_withoutLogin_approverIdStaysNull() {
        Long id = createDraftDocument("QM-PR-APPROVER-002");
        documentService.submitDocument(id);

        documentService.approveDocument(id, null);

        QmsDocumentDO saved = documentMapper.selectById(id);
        assertEquals(QmsDocStatusEnum.PUBLISHED.getStatus(), saved.getStatus());
        assertNull(saved.getApproverId(), "无会话时 getLoginUserId() 为 null，应由 updateById 忽略");
    }

    @Test
    public void test_approveChangeRequest_writesApproverId() {
        // 用 OBSOLETE 类型：只更新原文档状态，不走 REVISE/CREATE 的新版本 insert 路径
        Long docId = createDraftDocument("QM-PR-APPROVER-003");
        QmsDocumentChangeRequestSaveReqVO req = new QmsDocumentChangeRequestSaveReqVO();
        req.setDocumentId(docId);
        req.setChangeType(QmsDocChangeTypeEnum.OBSOLETE.getChangeType());
        req.setChangeReason("审批人落库测试");
        Long crId = changeRequestService.createChangeRequest(req);
        loginAs(USER_ID);

        changeRequestService.approveChangeRequest(crId);

        QmsDocumentChangeRequestDO saved = changeRequestMapper.selectById(crId);
        assertEquals(QmsChangeRequestStatusEnum.APPROVED.getStatus(), saved.getStatus());
        assertEquals(USER_ID, saved.getApproverId());
        assertNotNull(saved.getApproveDate());
    }
}
