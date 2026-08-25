package cn.zhicloud.module.qms.service.document;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentDistributePageReqVO;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentDistributeReturnReqVO;
import cn.zhicloud.module.qms.controller.admin.document.vo.QmsDocumentDistributeSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentDO;
import cn.zhicloud.module.qms.dal.dataobject.document.QmsDocumentDistributeDO;
import cn.zhicloud.module.qms.dal.mysql.document.QmsDocumentDistributeMapper;
import cn.zhicloud.module.qms.dal.mysql.document.QmsDocumentMapper;
import cn.zhicloud.module.qms.enums.document.QmsDocStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.DOCUMENT_DISTRIBUTE_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.DOCUMENT_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.DOCUMENT_STATUS_INVALID;

/**
 * QMS 文档分发记录 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class QmsDocumentDistributeServiceImpl implements QmsDocumentDistributeService {

    @Resource
    private QmsDocumentDistributeMapper distributeMapper;

    @Resource
    private QmsDocumentMapper documentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long distributeDocument(QmsDocumentDistributeSaveReqVO createReqVO) {
        // 1. 校验文档存在
        QmsDocumentDO document = documentMapper.selectById(createReqVO.getDocumentId());
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验文档状态=30 已发布
        if (!QmsDocStatusEnum.PUBLISHED.getStatus().equals(document.getStatus())) {
            throw exception(DOCUMENT_STATUS_INVALID);
        }
        // 3. 插入分发记录
        QmsDocumentDistributeDO distribute = BeanUtils.toBean(createReqVO, QmsDocumentDistributeDO.class);
        if (distribute.getReturnedQty() == null) {
            distribute.setReturnedQty(0);
        }
        distributeMapper.insert(distribute);
        return distribute.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDistribute(QmsDocumentDistributeSaveReqVO updateReqVO) {
        // 校验存在
        validateDistributeExists(updateReqVO.getId());
        // 更新
        QmsDocumentDistributeDO updateObj = BeanUtils.toBean(updateReqVO, QmsDocumentDistributeDO.class);
        distributeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDistribute(Long id) {
        // 校验存在
        validateDistributeExists(id);
        // 删除
        distributeMapper.deleteById(id);
    }

    private void validateDistributeExists(Long id) {
        if (distributeMapper.selectById(id) == null) {
            throw exception(DOCUMENT_DISTRIBUTE_NOT_EXISTS);
        }
    }

    @Override
    public QmsDocumentDistributeDO getDistribute(Long id) {
        return distributeMapper.selectById(id);
    }

    @Override
    public PageResult<QmsDocumentDistributeDO> getDistributePage(QmsDocumentDistributePageReqVO pageReqVO) {
        return distributeMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnDocument(QmsDocumentDistributeReturnReqVO reqVO) {
        // 1. 校验存在
        QmsDocumentDistributeDO distribute = distributeMapper.selectById(reqVO.getId());
        if (distribute == null) {
            throw exception(DOCUMENT_DISTRIBUTE_NOT_EXISTS);
        }
        // 2. 更新回收份数与回收日期
        QmsDocumentDistributeDO updateObj = new QmsDocumentDistributeDO();
        updateObj.setId(reqVO.getId());
        updateObj.setReturnedQty(reqVO.getReturnedQty());
        updateObj.setReturnedDate(reqVO.getReturnedDate());
        distributeMapper.updateById(updateObj);
    }

    @Override
    public List<QmsDocumentDistributeDO> getDistributeListByDocumentId(Long documentId) {
        return distributeMapper.selectListByDocumentId(documentId);
    }

}
