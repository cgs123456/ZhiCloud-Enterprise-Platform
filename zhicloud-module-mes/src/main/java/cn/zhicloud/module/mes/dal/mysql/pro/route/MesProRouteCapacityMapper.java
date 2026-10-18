package cn.zhicloud.module.mes.dal.mysql.pro.route;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteCapacityDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MES 工艺路线产能 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesProRouteCapacityMapper extends BaseMapperX<MesProRouteCapacityDO> {

    default MesProRouteCapacityDO selectByRouteId(Long routeId) {
        return selectOne(MesProRouteCapacityDO::getRouteId, routeId);
    }

    /**
     * 按工艺路线物理删除产能行（级联冲洗，绕过 {@code @TableLogic}）
     *
     * §12.270 D5：deleteRoute 级联专用——逻辑删留下的 (tenant_id, route_id) tombstone
     * 会占用唯一键 uk_mes_pro_route_capacity_route，故必须物理删除（V205/V211 库存冲洗先例）。
     */
    @Delete("DELETE FROM mes_pro_route_capacity WHERE route_id = #{routeId}")
    int deleteByRouteIdPhysical(@Param("routeId") Long routeId);

}
