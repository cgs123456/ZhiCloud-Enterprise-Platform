package cn.zhicloud.module.mes.service.dv.tp;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanItemSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import cn.zhicloud.module.mes.dal.mysql.dv.tp.MesDvTpPlanItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.DV_TP_PLAN_ITEM_NOT_EXISTS;

/**
 * MES TPM 计划项目 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesDvTpPlanItemServiceImpl implements MesDvTpPlanItemService {

    @Resource
    private MesDvTpPlanItemMapper tpPlanItemMapper;

    @Override
    public Long addPlanItem(MesDvTpPlanItemSaveReqVO createReqVO) {
        MesDvTpPlanItemDO item = BeanUtils.toBean(createReqVO, MesDvTpPlanItemDO.class);
        tpPlanItemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void updatePlanItem(MesDvTpPlanItemSaveReqVO updateReqVO) {
        validateTpPlanItem(updateReqVO.getId());
        MesDvTpPlanItemDO updateObj = BeanUtils.toBean(updateReqVO, MesDvTpPlanItemDO.class);
        tpPlanItemMapper.updateById(updateObj);
    }

    @Override
    public void deletePlanItem(Long id) {
        validateTpPlanItem(id);
        tpPlanItemMapper.deleteById(id);
    }

    @Override
    public MesDvTpPlanItemDO getTpPlanItem(Long id) {
        return tpPlanItemMapper.selectById(id);
    }

    @Override
    public List<MesDvTpPlanItemDO> getTpPlanItemListByPlanId(Long planId) {
        return tpPlanItemMapper.selectListByPlanId(planId);
    }

    private MesDvTpPlanItemDO validateTpPlanItem(Long id) {
        MesDvTpPlanItemDO item = tpPlanItemMapper.selectById(id);
        if (item == null) {
            throw exception(DV_TP_PLAN_ITEM_NOT_EXISTS);
        }
        return item;
    }

}