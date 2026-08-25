package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpProfitCenterDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpProfitCenterMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 利润中心 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpProfitCenterServiceImpl implements ErpProfitCenterService {

    @Resource
    private ErpProfitCenterMapper profitCenterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProfitCenter(ErpProfitCenterSaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 校验父级
        validateParent(createReqVO.getParentId(), null);
        // 插入
        ErpProfitCenterDO profitCenter = BeanUtils.toBean(createReqVO, ErpProfitCenterDO.class);
        profitCenterMapper.insert(profitCenter);
        return profitCenter.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfitCenter(ErpProfitCenterSaveReqVO updateReqVO) {
        // 校验存在
        validateProfitCenterExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 校验父级
        validateParent(updateReqVO.getParentId(), updateReqVO.getId());
        // 更新
        ErpProfitCenterDO updateObj = BeanUtils.toBean(updateReqVO, ErpProfitCenterDO.class);
        profitCenterMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProfitCenter(Long id) {
        // 校验存在
        validateProfitCenterExists(id);
        // 检查是否有子利润中心
        List<ErpProfitCenterDO> children = profitCenterMapper.selectListByParentId(id);
        if (children != null && !children.isEmpty()) {
            ErpProfitCenterDO center = profitCenterMapper.selectById(id);
            throw exception(PROFIT_CENTER_HAS_CHILDREN, center == null ? id : center.getCode());
        }
        // 删除
        profitCenterMapper.deleteById(id);
    }

    @Override
    public ErpProfitCenterDO getProfitCenter(Long id) {
        return profitCenterMapper.selectById(id);
    }

    @Override
    public PageResult<ErpProfitCenterDO> getProfitCenterPage(ErpProfitCenterPageReqVO pageReqVO) {
        return profitCenterMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpProfitCenterDO> getProfitCenterList() {
        return profitCenterMapper.selectList(null);
    }

    private void validateProfitCenterExists(Long id) {
        if (profitCenterMapper.selectById(id) == null) {
            throw exception(PROFIT_CENTER_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        ErpProfitCenterDO existing = profitCenterMapper.selectByCode(code);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(PROFIT_CENTER_CODE_DUPLICATE, code);
        }
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId <= 0L) {
            return; // 顶级
        }
        if (selfId != null && Objects.equals(parentId, selfId)) {
            throw exception(PROFIT_CENTER_PARENT_ERROR);
        }
        if (profitCenterMapper.selectById(parentId) == null) {
            throw exception(PROFIT_CENTER_PARENT_NOT_EXITS);
        }
    }

}
