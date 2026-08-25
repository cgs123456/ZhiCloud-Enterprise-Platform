package cn.zhicloud.module.iot.dal.mysql.ota;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.iot.controller.admin.ota.vo.firmware.IotOtaFirmwarePageReqVO;
import cn.zhicloud.module.iot.dal.dataobject.ota.IotOtaFirmwareDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IotOtaFirmwareMapper extends BaseMapperX<IotOtaFirmwareDO> {

    default IotOtaFirmwareDO selectByProductIdAndVersion(Long productId, String version) {
        return selectOne(IotOtaFirmwareDO::getProductId, productId,
                IotOtaFirmwareDO::getVersion, version);
    }

    default PageResult<IotOtaFirmwareDO> selectPage(IotOtaFirmwarePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<IotOtaFirmwareDO>()
                .likeIfPresent(IotOtaFirmwareDO::getName, pageReqVO.getName())
                .eqIfPresent(IotOtaFirmwareDO::getProductId, pageReqVO.getProductId())
                .betweenIfPresent(IotOtaFirmwareDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(IotOtaFirmwareDO::getCreateTime));
    }

}
