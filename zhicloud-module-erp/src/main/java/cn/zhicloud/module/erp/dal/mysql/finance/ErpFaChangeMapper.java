package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fachange.ErpFaChangePageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFaChangeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 固定资产变动记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpFaChangeMapper extends BaseMapperX<ErpFaChangeDO> {

    default List<ErpFaChangeDO> selectListByAssetId(Long assetId) {
        return selectList(ErpFaChangeDO::getAssetId, assetId);
    }

    default PageResult<ErpFaChangeDO> selectPage(ErpFaChangePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpFaChangeDO>()
                .eqIfPresent(ErpFaChangeDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(ErpFaChangeDO::getChangeType, reqVO.getChangeType())
                .eqIfPresent(ErpFaChangeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpFaChangeDO::getChangeDate, reqVO.getChangeDate())
                .orderByDesc(ErpFaChangeDO::getId));
    }

}
