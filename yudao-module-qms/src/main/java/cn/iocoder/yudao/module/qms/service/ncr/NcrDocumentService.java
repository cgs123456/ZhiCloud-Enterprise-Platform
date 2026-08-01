package cn.iocoder.yudao.module.qms.service.ncr;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.ncr.vo.NcrDispositionReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.ncr.vo.NcrDocumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.ncr.vo.NcrDocumentSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.ncr.NcrDocumentDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.ncr.NcrMrbRecordDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 不合格品报告 Service 接口
 *
 * @author 芋道源码
 */
public interface NcrDocumentService {

    /**
     * 创建 NCR 报告
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createNcrDocument(@Valid NcrDocumentSaveReqVO createReqVO);

    /**
     * 更新 NCR 报告
     *
     * @param updateReqVO 更新信息
     */
    void updateNcrDocument(@Valid NcrDocumentSaveReqVO updateReqVO);

    /**
     * 删除 NCR 报告
     *
     * @param id 编号
     */
    void deleteNcrDocument(Long id);

    /**
     * 获得 NCR 报告
     *
     * @param id 编号
     * @return NCR 报告
     */
    NcrDocumentDO getNcrDocument(Long id);

    /**
     * 获得 NCR 报告分页
     *
     * @param pageReqVO 分页查询
     * @return NCR 报告分页
     */
    PageResult<NcrDocumentDO> getNcrDocumentPage(NcrDocumentPageReqVO pageReqVO);

    /**
     * 提交 MRB 评审
     *
     * <p>仅在 status=OPEN 时允许，提交后状态流转为 MRB_REVIEW。
     *
     * @param ncrId NCR 编号
     */
    void submitForMrb(Long ncrId);

    /**
     * 记录处置决议
     *
     * <p>仅在 status=MRB_REVIEW 时允许，记录 MRB 决议并流转为 DISPOSITIONED。
     * 决议中的 decision 同步到 NCR 报告的 disposition 字段。
     *
     * @param reqVO 处置决议
     */
    void recordDisposition(@Valid NcrDispositionReqVO reqVO);

    /**
     * 关闭 NCR 报告
     *
     * <p>仅在 status=DISPOSITIONED 时允许，关闭后状态流转为 CLOSED。
     *
     * @param id 编号
     */
    void closeNcrDocument(Long id);

    /**
     * 获得 NCR 报告关联的 MRB 评审记录列表
     *
     * @param ncrId NCR 编号
     * @return MRB 评审记录列表
     */
    List<NcrMrbRecordDO> getMrbRecordListByNcrId(Long ncrId);

}
