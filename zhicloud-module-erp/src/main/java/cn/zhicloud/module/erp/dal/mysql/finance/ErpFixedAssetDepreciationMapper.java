package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFixedAssetDepreciationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 固定资产折旧记录 Mapper（P0-14）
 *
 * @author 智云
 */
@Mapper
public interface ErpFixedAssetDepreciationMapper extends BaseMapperX<ErpFixedAssetDepreciationDO> {

    default List<ErpFixedAssetDepreciationDO> selectListByFixedAssetId(Long fixedAssetId) {
        return selectList(ErpFixedAssetDepreciationDO::getFixedAssetId, fixedAssetId);
    }

    default ErpFixedAssetDepreciationDO selectByFixedAssetIdAndPeriodId(Long fixedAssetId, Long periodId) {
        return selectOne(new LambdaQueryWrapperX<ErpFixedAssetDepreciationDO>()
                .eq(ErpFixedAssetDepreciationDO::getFixedAssetId, fixedAssetId)
                .eq(ErpFixedAssetDepreciationDO::getPeriodId, periodId));
    }

    default List<ErpFixedAssetDepreciationDO> selectListByPeriodId(Long periodId) {
        return selectList(ErpFixedAssetDepreciationDO::getPeriodId, periodId);
    }

    default List<ErpFixedAssetDepreciationDO> selectListByPeriodIdAndStatus(Long periodId, Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpFixedAssetDepreciationDO>()
                .eq(ErpFixedAssetDepreciationDO::getPeriodId, periodId)
                .eq(ErpFixedAssetDepreciationDO::getStatus, status));
    }

}
