package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCostCenterDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpCostCenterMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 成本中心 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpCostCenterServiceImpl implements ErpCostCenterService {

    @Resource
    private ErpCostCenterMapper costCenterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCostCenter(ErpCostCenterSaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 校验父级
        validateParent(createReqVO.getParentId(), null);
        // 插入
        ErpCostCenterDO costCenter = BeanUtils.toBean(createReqVO, ErpCostCenterDO.class);
        costCenterMapper.insert(costCenter);
        return costCenter.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCostCenter(ErpCostCenterSaveReqVO updateReqVO) {
        // 校验存在
        validateCostCenterExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 校验父级
        validateParent(updateReqVO.getParentId(), updateReqVO.getId());
        // 更新
        ErpCostCenterDO updateObj = BeanUtils.toBean(updateReqVO, ErpCostCenterDO.class);
        costCenterMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCostCenter(Long id) {
        // 校验存在
        validateCostCenterExists(id);
        // 检查是否有子成本中心
        List<ErpCostCenterDO> children = costCenterMapper.selectListByParentId(id);
        if (children != null && !children.isEmpty()) {
            ErpCostCenterDO center = costCenterMapper.selectById(id);
            throw exception(COST_CENTER_HAS_CHILDREN, center == null ? id : center.getCode());
        }
        // 删除
        costCenterMapper.deleteById(id);
    }

    @Override
    public ErpCostCenterDO getCostCenter(Long id) {
        return costCenterMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCostCenterDO> getCostCenterPage(ErpCostCenterPageReqVO pageReqVO) {
        return costCenterMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCostCenterDO> getCostCenterList() {
        return costCenterMapper.selectList(null);
    }

    private void validateCostCenterExists(Long id) {
        if (costCenterMapper.selectById(id) == null) {
            throw exception(COST_CENTER_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        ErpCostCenterDO existing = costCenterMapper.selectByCode(code);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(COST_CENTER_CODE_DUPLICATE, code);
        }
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId <= 0L) {
            return; // 顶级
        }
        if (selfId != null && Objects.equals(parentId, selfId)) {
            throw exception(COST_CENTER_PARENT_ERROR);
        }
        if (costCenterMapper.selectById(parentId) == null) {
            throw exception(COST_CENTER_PARENT_NOT_EXITS);
        }
    }

}
