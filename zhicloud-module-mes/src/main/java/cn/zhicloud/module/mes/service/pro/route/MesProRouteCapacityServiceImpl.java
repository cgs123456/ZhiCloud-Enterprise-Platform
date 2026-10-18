package cn.zhicloud.module.mes.service.pro.route;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.pro.route.vo.capacity.MesProRouteCapacitySaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteCapacityDO;
import cn.zhicloud.module.mes.dal.mysql.pro.route.MesProRouteCapacityMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * MES 工艺路线产能 Service 实现类
 *
 * upsert 口径：按 routeId 查无则插、有则更；唯一键 uk(tenant_id, route_id) 兜底并发。
 *
 * @author 智云
 */
@Service
@Validated
public class MesProRouteCapacityServiceImpl implements MesProRouteCapacityService {

    @Resource
    private MesProRouteCapacityMapper routeCapacityMapper;

    @Resource
    @Lazy
    private MesProRouteService routeService;

    @Override
    public MesProRouteCapacityDO getRouteCapacityByRouteId(Long routeId) {
        return routeCapacityMapper.selectByRouteId(routeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRouteCapacity(MesProRouteCapacitySaveReqVO saveReqVO) {
        // 1.0 已启用的工艺路线，不允许操作（子表惯例）
        routeService.validateRouteNotEnable(saveReqVO.getRouteId());
        // 1.1 upsert：未配置则新增，已配置则更新（唯一键 uk(tenant_id, route_id) 兜底）
        MesProRouteCapacityDO existing = routeCapacityMapper.selectByRouteId(saveReqVO.getRouteId());
        if (existing == null) {
            MesProRouteCapacityDO capacity = BeanUtils.toBean(saveReqVO, MesProRouteCapacityDO.class);
            routeCapacityMapper.insert(capacity);
            return capacity.getId();
        }
        MesProRouteCapacityDO updateObj = BeanUtils.toBean(saveReqVO, MesProRouteCapacityDO.class);
        updateObj.setId(existing.getId());
        routeCapacityMapper.updateById(updateObj);
        return existing.getId();
    }

    @Override
    public void deleteRouteCapacityByRouteId(Long routeId) {
        routeCapacityMapper.deleteByRouteIdPhysical(routeId);
    }

}
