package cn.iocoder.yudao.module.qms.service.document;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentDistributePageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentDistributeReturnReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.document.vo.QmsDocumentDistributeSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.document.QmsDocumentDistributeDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 文档分发记录 Service 接口
 *
 * @author 芋道源码
 */
public interface QmsDocumentDistributeService {

    /**
     * 创建分发记录（校验文档状态=30 已发布）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long distributeDocument(@Valid QmsDocumentDistributeSaveReqVO createReqVO);

    /**
     * 更新分发记录
     *
     * @param updateReqVO 更新信息
     */
    void updateDistribute(@Valid QmsDocumentDistributeSaveReqVO updateReqVO);

    /**
     * 删除分发记录
     *
     * @param id 编号
     */
    void deleteDistribute(Long id);

    /**
     * 获得分发记录
     *
     * @param id 编号
     * @return 分发记录
     */
    QmsDocumentDistributeDO getDistribute(Long id);

    /**
     * 获得分发记录分页
     *
     * @param pageReqVO 分页查询
     * @return 分发记录分页
     */
    PageResult<QmsDocumentDistributeDO> getDistributePage(QmsDocumentDistributePageReqVO pageReqVO);

    /**
     * 回收登记（更新 returned_qty 和 returned_date）
     *
     * @param reqVO 回收信息
     */
    void returnDocument(@Valid QmsDocumentDistributeReturnReqVO reqVO);

    /**
     * 获得文档关联的分发记录列表
     *
     * @param documentId 受控文档 ID
     * @return 分发记录列表
     */
    List<QmsDocumentDistributeDO> getDistributeListByDocumentId(Long documentId);

}
