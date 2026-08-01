package cn.iocoder.yudao.module.mes.dal.mysql.dv.scada;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.scada.MesDvScadaConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES SCADA 设备配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesDvScadaConfigMapper extends BaseMapperX<MesDvScadaConfigDO> {

    default PageResult<MesDvScadaConfigDO> selectPage(MesDvScadaConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvScadaConfigDO>()
                .eqIfPresent(MesDvScadaConfigDO::getMachineryId, reqVO.getMachineryId())
                .eqIfPresent(MesDvScadaConfigDO::getIotDevicePk, reqVO.getIotDevicePk())
                .eqIfPresent(MesDvScadaConfigDO::getProtocolType, reqVO.getProtocolType())
                .eqIfPresent(MesDvScadaConfigDO::getEnabled, reqVO.getEnabled())
                .orderByDesc(MesDvScadaConfigDO::getId));
    }

    default MesDvScadaConfigDO selectByMachineryId(Long machineryId) {
        return selectOne(MesDvScadaConfigDO::getMachineryId, machineryId);
    }

}
