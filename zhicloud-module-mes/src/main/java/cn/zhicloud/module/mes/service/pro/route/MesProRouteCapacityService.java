package cn.zhicloud.module.mes.service.pro.route;

import cn.zhicloud.module.mes.controller.admin.pro.route.vo.capacity.MesProRouteCapacitySaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteCapacityDO;

/**
 * MES 工艺路线产能 Service 接口
 *
 * @author 智云
 */
public interface MesProRouteCapacityService {

    /**
     * 按工艺路线获取产能配置
     *
     * @param routeId 工艺路线编号
     * @return 产能配置（未配置返回 null，调用方回退）
     */
    MesProRouteCapacityDO getRouteCapacityByRouteId(Long routeId);

    /**
     * 保存工艺路线产能（upsert：未配置则新增，已配置则更新）
     *
     * 护栏：已启用的工艺路线不允许操作（子表惯例，validateRouteNotEnable）。
     *
     * @param saveReqVO 保存信息
     * @return 产能配置编号
     */
    Long saveRouteCapacity(MesProRouteCapacitySaveReqVO saveReqVO);

    /**
     * 按工艺路线删除产能配置（级联冲洗，物理删除）
     *
     * @param routeId 工艺路线编号
     */
    void deleteRouteCapacityByRouteId(Long routeId);

}
