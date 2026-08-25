package cn.zhicloud.module.mes.dal.mysql.dv.scada;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvDeviceDataRecordPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.scada.MesDvDeviceDataRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES 设备数据采集记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesDvDeviceDataRecordMapper extends BaseMapperX<MesDvDeviceDataRecordDO> {

    default PageResult<MesDvDeviceDataRecordDO> selectPage(MesDvDeviceDataRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvDeviceDataRecordDO>()
                .eqIfPresent(MesDvDeviceDataRecordDO::getMachineryId, reqVO.getMachineryId())
                .eqIfPresent(MesDvDeviceDataRecordDO::getScadaConfigId, reqVO.getScadaConfigId())
                .likeIfPresent(MesDvDeviceDataRecordDO::getPropertyName, reqVO.getPropertyName())
                .eqIfPresent(MesDvDeviceDataRecordDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesDvDeviceDataRecordDO::getCollectTime, reqVO.getCollectTime())
                .orderByDesc(MesDvDeviceDataRecordDO::getCollectTime));
    }

    default List<MesDvDeviceDataRecordDO> selectListByMachineryAndTimeRange(Long machineryId,
                                                                            LocalDateTime start,
                                                                            LocalDateTime end) {
        return selectList(new LambdaQueryWrapperX<MesDvDeviceDataRecordDO>()
                .eq(MesDvDeviceDataRecordDO::getMachineryId, machineryId)
                .between(MesDvDeviceDataRecordDO::getCollectTime, start, end)
                .orderByAsc(MesDvDeviceDataRecordDO::getCollectTime));
    }

    default MesDvDeviceDataRecordDO selectLatestByMachineryId(Long machineryId) {
        return selectOne(new LambdaQueryWrapperX<MesDvDeviceDataRecordDO>()
                .eq(MesDvDeviceDataRecordDO::getMachineryId, machineryId)
                .orderByDesc(MesDvDeviceDataRecordDO::getCollectTime)
                .last("LIMIT 1"));
    }

}
