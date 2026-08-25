package cn.zhicloud.module.mes.dal.mysql.wm.salesnotice;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.wm.salesnotice.vo.MesWmSalesNoticePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.salesnotice.MesWmSalesNoticeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 发货通知单 Mapper
 */
@Mapper
public interface MesWmSalesNoticeMapper extends BaseMapperX<MesWmSalesNoticeDO> {

    default PageResult<MesWmSalesNoticeDO> selectPage(MesWmSalesNoticePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmSalesNoticeDO>()
                .likeIfPresent(MesWmSalesNoticeDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmSalesNoticeDO::getName, reqVO.getName())
                .likeIfPresent(MesWmSalesNoticeDO::getSalesOrderCode, reqVO.getSalesOrderCode())
                .eqIfPresent(MesWmSalesNoticeDO::getClientId, reqVO.getClientId())
                .eqIfPresent(MesWmSalesNoticeDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesWmSalesNoticeDO::getId));
    }

    default MesWmSalesNoticeDO selectByCode(String code) {
        return selectOne(MesWmSalesNoticeDO::getCode, code);
    }

}
