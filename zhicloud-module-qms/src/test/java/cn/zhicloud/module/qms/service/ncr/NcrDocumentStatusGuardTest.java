package cn.zhicloud.module.qms.service.ncr;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.ncr.vo.NcrDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.ncr.NcrDocumentDO;
import cn.zhicloud.module.qms.dal.mysql.ncr.NcrDocumentMapper;
import cn.zhicloud.module.qms.enums.qms.NcrDispositionEnum;
import cn.zhicloud.module.qms.enums.qms.NcrStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：NCR 建单/改单状态与处置结论护栏测试。
 *
 * <p>此前 createNcrDocument 只在字段为 null 时补默认值，于是调用方可以在**建单**这一步
 * 直接写入 CLOSED(40) 与 disposition=USE_AS_IS(50)/RETURN(60)，
 * 造出一张从未走 MRB 评审、也没有任何 {@code NcrMrbRecordDO} 留痕的「已关闭」不合格品报告。
 * MRB 记录本身就是处置决策的证据，跳过它等于证据凭空产生。
 *
 * <p>状态与处置结论只能由 submitForMrb / recordDisposition / closeNcrDocument 产生。
 *
 * @author 智云
 */
@Import(NcrDocumentServiceImpl.class)
public class NcrDocumentStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private NcrDocumentServiceImpl ncrService;

    @Resource
    private NcrDocumentMapper ncrDocumentMapper;

    @Test
    public void test_createNcrDocument_ignoresSuppliedClosedStatus() {
        NcrDocumentSaveReqVO req = new NcrDocumentSaveReqVO();
        req.setStatus(NcrStatusEnum.CLOSED.getStatus());

        Long id = ncrService.createNcrDocument(req);

        NcrDocumentDO saved = ncrDocumentMapper.selectById(id);
        assertEquals(NcrStatusEnum.OPEN.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createNcrDocument_ignoresSuppliedDisposition() {
        // 攻击形态：建单即写入处置结论（等同跳过 MRB 评审）
        NcrDocumentSaveReqVO req = new NcrDocumentSaveReqVO();
        req.setStatus(NcrStatusEnum.DISPOSITIONED.getStatus());
        req.setDisposition(NcrDispositionEnum.USE_AS_IS.getDisposition());

        Long id = ncrService.createNcrDocument(req);

        NcrDocumentDO saved = ncrDocumentMapper.selectById(id);
        assertEquals(NcrStatusEnum.OPEN.getStatus(), saved.getStatus());
        assertNull(saved.getDisposition(), "处置结论必须由 recordDisposition 产生，建单不得写入");
    }

    @Test
    public void test_createNcrDocument_defaultStatusIsOpen() {
        NcrDocumentSaveReqVO req = new NcrDocumentSaveReqVO();

        Long id = ncrService.createNcrDocument(req);

        NcrDocumentDO saved = ncrDocumentMapper.selectById(id);
        assertEquals(NcrStatusEnum.OPEN.getStatus(), saved.getStatus());
        assertNull(saved.getDisposition());
    }

    @Test
    public void test_updateNcrDocument_ignoresSuppliedStatusAndDisposition() {
        NcrDocumentDO doc = randomPojo(NcrDocumentDO.class, o -> {
            o.setStatus(NcrStatusEnum.OPEN.getStatus());
            o.setDisposition(null);
        });
        ncrDocumentMapper.insert(doc);

        NcrDocumentSaveReqVO req = new NcrDocumentSaveReqVO();
        req.setId(doc.getId());
        req.setStatus(NcrStatusEnum.CLOSED.getStatus());
        req.setDisposition(NcrDispositionEnum.RETURN.getDisposition());

        ncrService.updateNcrDocument(req);

        // 同上：updateById 跳过 null 字段，所以断言的是「未被改写」而非「变成 null」
        NcrDocumentDO updated = ncrDocumentMapper.selectById(doc.getId());
        assertEquals(NcrStatusEnum.OPEN.getStatus(), updated.getStatus());
        assertEquals(doc.getDisposition(), updated.getDisposition());
    }

    @Test
    public void test_deleteNcrDocument_notExists() {
        assertThrows(Exception.class, () -> ncrService.deleteNcrDocument(randomLongId()));
    }
}
