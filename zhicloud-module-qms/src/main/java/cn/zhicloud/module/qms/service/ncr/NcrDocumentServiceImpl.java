package cn.zhicloud.module.qms.service.ncr;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.ncr.vo.NcrDispositionReqVO;
import cn.zhicloud.module.qms.controller.admin.ncr.vo.NcrDocumentPageReqVO;
import cn.zhicloud.module.qms.controller.admin.ncr.vo.NcrDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.ncr.NcrDocumentDO;
import cn.zhicloud.module.qms.dal.dataobject.ncr.NcrMrbRecordDO;
import cn.zhicloud.module.qms.dal.mysql.ncr.NcrDocumentMapper;
import cn.zhicloud.module.qms.dal.mysql.ncr.NcrMrbRecordMapper;
import cn.zhicloud.module.qms.enums.qms.NcrMrbDecisionEnum;
import cn.zhicloud.module.qms.enums.qms.NcrStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.*;

/**
 * QMS 不合格品报告 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class NcrDocumentServiceImpl implements NcrDocumentService {

    @Resource
    private NcrDocumentMapper ncrDocumentMapper;

    @Resource
    private NcrMrbRecordMapper ncrMrbRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNcrDocument(NcrDocumentSaveReqVO createReqVO) {
        // 插入
        NcrDocumentDO ncrDocument = BeanUtils.toBean(createReqVO, NcrDocumentDO.class);
        // 状态与处置结论无条件重置为初始态，忽略调用方传入的任何值。
        //
        // §12.235.5（critical）：此前「仅在 null 时补默认」，于是建单即可直接传入
        // CLOSED(40) 与 disposition=USE_AS_IS(50)/RETURN(60)，
        // 造出一张从未走 MRB 评审、也没有任何 NcrMrbRecordDO 留痕的「已关闭」不合格品报告。
        // MRB 记录是处置决策的证据本身，跳过它等于证据凭空产生。
        // 状态只能由 submitForMrb/recordDisposition/closeNcrDocument 产生。
        ncrDocument.setStatus(NcrStatusEnum.OPEN.getStatus());
        ncrDocument.setDisposition(null);
        ncrDocumentMapper.insert(ncrDocument);
        // 返回
        return ncrDocument.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNcrDocument(NcrDocumentSaveReqVO updateReqVO) {
        // 校验存在
        validateNcrDocumentExists(updateReqVO.getId());
        // 更新
        NcrDocumentDO updateObj = BeanUtils.toBean(updateReqVO, NcrDocumentDO.class);
        // 禁止通过通用更新修改状态与处置结论，二者必须走
        // submitForMrb/recordDisposition/closeNcrDocument 等状态流转方法（§12.235.5）
        updateObj.setStatus(null);
        updateObj.setDisposition(null);
        ncrDocumentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNcrDocument(Long id) {
        // 校验存在
        validateNcrDocumentExists(id);
        // 删除
        ncrDocumentMapper.deleteById(id);
    }

    private void validateNcrDocumentExists(Long id) {
        if (ncrDocumentMapper.selectById(id) == null) {
            throw exception(NCR_DOCUMENT_NOT_EXISTS);
        }
    }

    @Override
    public NcrDocumentDO getNcrDocument(Long id) {
        return ncrDocumentMapper.selectById(id);
    }

    @Override
    public PageResult<NcrDocumentDO> getNcrDocumentPage(NcrDocumentPageReqVO pageReqVO) {
        return ncrDocumentMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForMrb(Long ncrId) {
        // 1. 校验存在
        NcrDocumentDO document = ncrDocumentMapper.selectById(ncrId);
        if (document == null) {
            throw exception(NCR_DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验状态：必须是 OPEN
        if (!NcrStatusEnum.OPEN.getStatus().equals(document.getStatus())) {
            throw exception(NCR_DOCUMENT_NOT_SUBMIT_MRB);
        }
        // 3. 流转状态为 MRB_REVIEW
        NcrDocumentDO updateObj = new NcrDocumentDO();
        updateObj.setId(ncrId);
        updateObj.setStatus(NcrStatusEnum.MRB_REVIEW.getStatus());
        ncrDocumentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordDisposition(NcrDispositionReqVO reqVO) {
        // 1. 校验存在
        NcrDocumentDO document = ncrDocumentMapper.selectById(reqVO.getNcrId());
        if (document == null) {
            throw exception(NCR_DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验状态：必须是 MRB_REVIEW
        if (!NcrStatusEnum.MRB_REVIEW.getStatus().equals(document.getStatus())) {
            throw exception(NCR_DOCUMENT_NOT_MRB_REVIEW);
        }
        // 3. 插入 MRB 评审记录
        NcrMrbRecordDO mrbRecord = BeanUtils.toBean(reqVO, NcrMrbRecordDO.class);
        ncrMrbRecordMapper.insert(mrbRecord);
        // 4. 同步决议到 NCR 报告的 disposition 字段，并流转为 DISPOSITIONED
        Integer disposition = mapDecisionToDisposition(reqVO.getDecision());
        NcrDocumentDO updateObj = new NcrDocumentDO();
        updateObj.setId(document.getId());
        updateObj.setDisposition(disposition);
        updateObj.setStatus(NcrStatusEnum.DISPOSITIONED.getStatus());
        ncrDocumentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeNcrDocument(Long id) {
        // 1. 校验存在
        NcrDocumentDO document = ncrDocumentMapper.selectById(id);
        if (document == null) {
            throw exception(NCR_DOCUMENT_NOT_EXISTS);
        }
        // 2. 校验状态：必须是 DISPOSITIONED
        if (!NcrStatusEnum.DISPOSITIONED.getStatus().equals(document.getStatus())) {
            throw exception(NCR_DOCUMENT_NOT_DISPOSITIONED);
        }
        // 3. 流转状态为 CLOSED
        NcrDocumentDO updateObj = new NcrDocumentDO();
        updateObj.setId(id);
        updateObj.setStatus(NcrStatusEnum.CLOSED.getStatus());
        ncrDocumentMapper.updateById(updateObj);
    }

    @Override
    public List<NcrMrbRecordDO> getMrbRecordListByNcrId(Long ncrId) {
        return ncrMrbRecordMapper.selectListByNcrId(ncrId);
    }

    /**
     * 将 MRB 决议映射为 NCR 处置方式
     */
    private Integer mapDecisionToDisposition(Integer decision) {
        if (decision == null) {
            return null;
        }
        // MRB 决议与 NCR 处置方式一一对应，数值含义相同，直接透传
        return switch (decision) {
            case 10 -> 10; // ACCEPT_REWORK -> REWORK
            case 20 -> 20; // ACCEPT_REPAIR -> REPAIR
            case 30 -> 30; // ACCEPT_DEGRADE -> DEGRADE
            case 40 -> 40; // SCRAP -> SCRAP
            case 50 -> 50; // USE_AS_IS -> USE_AS_IS
            case 60 -> 60; // RETURN -> RETURN
            default -> null;
        };
    }

}
