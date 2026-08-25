package cn.zhicloud.module.wms.dal.mysql.order.crossdock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * WMS 越库单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsCrossDockOrderMapper extends BaseMapperX<WmsCrossDockOrderDO> {

    default PageResult<WmsCrossDockOrderDO> selectPage(WmsCrossDockOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsCrossDockOrderDO>()
                .likeIfPresent(WmsCrossDockOrderDO::getNo, reqVO.getNo())
                .eqIfPresent(WmsCrossDockOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(WmsCrossDockOrderDO::getSourceSupplierId, reqVO.getSourceSupplierId())
                .eqIfPresent(WmsCrossDockOrderDO::getTargetCustomerId, reqVO.getTargetCustomerId())
                .likeIfPresent(WmsCrossDockOrderDO::getReceiptOrderNo, reqVO.getReceiptOrderNo())
                .likeIfPresent(WmsCrossDockOrderDO::getShipmentOrderNo, reqVO.getShipmentOrderNo())
                .betweenIfPresent(WmsCrossDockOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsCrossDockOrderDO::getId));
    }

    default WmsCrossDockOrderDO selectByNo(String no) {
        return selectOne(WmsCrossDockOrderDO::getNo, no);
    }

    default int updateByIdAndStatus(Long id, Integer status, WmsCrossDockOrderDO updateObj) {
        return update(updateObj, new LambdaQueryWrapper<WmsCrossDockOrderDO>()
                .eq(WmsCrossDockOrderDO::getId, id)
                .eq(WmsCrossDockOrderDO::getStatus, status));
    }

}
