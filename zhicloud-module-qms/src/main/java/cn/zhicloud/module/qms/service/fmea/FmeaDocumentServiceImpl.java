package cn.zhicloud.module.qms.service.fmea;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.fmea.vo.FmeaDocumentPageReqVO;
import cn.zhicloud.module.qms.controller.admin.fmea.vo.FmeaDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.fmea.FmeaDocumentDO;
import cn.zhicloud.module.qms.dal.mysql.fmea.FmeaDocumentMapper;
import cn.zhicloud.module.qms.enums.qms.FmeaActionPriorityEnum;
import cn.zhicloud.module.qms.enums.qms.FmeaStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.FMEA_DOCUMENT_NOT_EXISTS;

/**
 * QMS FMEA 文档 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmeaDocumentServiceImpl implements FmeaDocumentService {

    @Resource
    private FmeaDocumentMapper fmeaDocumentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFmeaDocument(FmeaDocumentSaveReqVO createReqVO) {
        // 插入
        FmeaDocumentDO fmeaDocument = BeanUtils.toBean(createReqVO, FmeaDocumentDO.class);
        // 默认状态为草稿
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        fmeaDocument.setStatus(FmeaStatusEnum.DRAFT.getStatus());
        fmeaDocumentMapper.insert(fmeaDocument);
        // 返回
        return fmeaDocument.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFmeaDocument(FmeaDocumentSaveReqVO updateReqVO) {
        // 校验存在
        validateFmeaDocumentExists(updateReqVO.getId());
        // 更新
        FmeaDocumentDO updateObj = BeanUtils.toBean(updateReqVO, FmeaDocumentDO.class);
        // 禁止通过通用更新修改状态，状态变更必须走专门的状态流转方法
        updateObj.setStatus(null);
        fmeaDocumentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFmeaDocument(Long id) {
        // 校验存在
        validateFmeaDocumentExists(id);
        // 删除
        fmeaDocumentMapper.deleteById(id);
    }

    private void validateFmeaDocumentExists(Long id) {
        if (fmeaDocumentMapper.selectById(id) == null) {
            throw exception(FMEA_DOCUMENT_NOT_EXISTS);
        }
    }

    @Override
    public FmeaDocumentDO getFmeaDocument(Long id) {
        return fmeaDocumentMapper.selectById(id);
    }

    @Override
    public PageResult<FmeaDocumentDO> getFmeaDocumentPage(FmeaDocumentPageReqVO pageReqVO) {
        return fmeaDocumentMapper.selectPage(pageReqVO);
    }

    @Override
    public FmeaActionPriorityEnum calculateActionPriority(int severity, int occurrence, int detection) {
        // 委托 AIAG-VDA 2019 AP 矩阵查表计算器
        return FmeaActionPriorityCalculator.calculate(severity, occurrence, detection);
    }

    @Override
    public int calculateRpn(int severity, int occurrence, int detection) {
        // 旧版 FMEA：RPN = S × O × D（1-1000）
        return severity * occurrence * detection;
    }

}