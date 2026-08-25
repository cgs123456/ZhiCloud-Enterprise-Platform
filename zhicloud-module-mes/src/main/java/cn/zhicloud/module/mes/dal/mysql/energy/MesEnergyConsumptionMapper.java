package cn.zhicloud.module.mes.dal.mysql.energy;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.energy.vo.MesEnergyConsumptionPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.energy.MesEnergyConsumptionDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * MES 能源消耗记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesEnergyConsumptionMapper extends BaseMapperX<MesEnergyConsumptionDO> {

    default PageResult<MesEnergyConsumptionDO> selectPage(MesEnergyConsumptionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesEnergyConsumptionDO>()
                .eqIfPresent(MesEnergyConsumptionDO::getWorkshopId, reqVO.getWorkshopId())
                .eqIfPresent(MesEnergyConsumptionDO::getWorkstationId, reqVO.getWorkstationId())
                .eqIfPresent(MesEnergyConsumptionDO::getEnergyType, reqVO.getEnergyType())
                .betweenIfPresent(MesEnergyConsumptionDO::getRecordDate, reqVO.getRecordDate())
                .orderByDesc(MesEnergyConsumptionDO::getRecordDate));
    }

    /**
     * 查询指定车间在日期范围内的能源消耗
     */
    default List<MesEnergyConsumptionDO> selectListByWorkshopAndDateRange(Long workshopId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<MesEnergyConsumptionDO>()
                .eq(MesEnergyConsumptionDO::getWorkshopId, workshopId)
                .ge(MesEnergyConsumptionDO::getRecordDate, startDate)
                .le(MesEnergyConsumptionDO::getRecordDate, endDate)
                .orderByAsc(MesEnergyConsumptionDO::getRecordDate));
    }

}
