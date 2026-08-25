package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpConsolidationWorksheetMapper;
import cn.zhicloud.module.erp.enums.finance.ErpWorksheetStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 合并工作底稿 Service 实现类（P1-合并报表引擎）
 *
 * @author 智云
 */
@Service
@Validated
public class ErpConsolidationWorksheetServiceImpl implements ErpConsolidationWorksheetService {

    @Resource
    private ErpConsolidationWorksheetMapper consolidationWorksheetMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorksheet(ErpConsolidationWorksheetSaveReqVO createReqVO) {
        ErpConsolidationWorksheetDO worksheet = BeanUtils.toBean(createReqVO, ErpConsolidationWorksheetDO.class);
        if (worksheet.getStatus() == null) {
            worksheet.setStatus(ErpWorksheetStatusEnum.PENDING.getStatus());
        }
        consolidationWorksheetMapper.insert(worksheet);
        return worksheet.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorksheet(ErpConsolidationWorksheetSaveReqVO updateReqVO) {
        ErpConsolidationWorksheetDO existing = validateWorksheetExists(updateReqVO.getId());
        // 仅待审核状态可修改
        if (!Objects.equals(existing.getStatus(), ErpWorksheetStatusEnum.PENDING.getStatus())) {
            throw exception(CONSOLIDATION_WORKSHEET_NOT_EXISTS);
        }
        ErpConsolidationWorksheetDO updateObj = BeanUtils.toBean(updateReqVO, ErpConsolidationWorksheetDO.class);
        // 保持原状态，不允许通过更新接口变更状态
        updateObj.setStatus(ErpWorksheetStatusEnum.PENDING.getStatus());
        consolidationWorksheetMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorksheet(Long id) {
        validateWorksheetExists(id);
        consolidationWorksheetMapper.deleteById(id);
    }

    @Override
    public ErpConsolidationWorksheetDO getWorksheet(Long id) {
        return consolidationWorksheetMapper.selectById(id);
    }

    @Override
    public PageResult<ErpConsolidationWorksheetDO> getWorksheetPage(ErpConsolidationWorksheetPageReqVO pageReqVO) {
        return consolidationWorksheetMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpConsolidationWorksheetDO> getWorksheetListByPeriod(String consolidationPeriod) {
        return consolidationWorksheetMapper.selectListByPeriod(consolidationPeriod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWorksheet(Long id) {
        ErpConsolidationWorksheetDO existing = validateWorksheetExists(id);
        if (Objects.equals(existing.getStatus(), ErpWorksheetStatusEnum.APPROVED.getStatus())) {
            return;
        }
        ErpConsolidationWorksheetDO updateObj = new ErpConsolidationWorksheetDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpWorksheetStatusEnum.APPROVED.getStatus());
        consolidationWorksheetMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectWorksheet(Long id) {
        ErpConsolidationWorksheetDO existing = validateWorksheetExists(id);
        if (Objects.equals(existing.getStatus(), ErpWorksheetStatusEnum.REJECTED.getStatus())) {
            return;
        }
        ErpConsolidationWorksheetDO updateObj = new ErpConsolidationWorksheetDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpWorksheetStatusEnum.REJECTED.getStatus());
        consolidationWorksheetMapper.updateById(updateObj);
    }

    // ==================== 内部辅助方法 ====================

    private ErpConsolidationWorksheetDO validateWorksheetExists(Long id) {
        if (id == null) {
            throw exception(CONSOLIDATION_WORKSHEET_NOT_EXISTS);
        }
        ErpConsolidationWorksheetDO worksheet = consolidationWorksheetMapper.selectById(id);
        if (worksheet == null) {
            throw exception(CONSOLIDATION_WORKSHEET_NOT_EXISTS);
        }
        return worksheet;
    }

}
