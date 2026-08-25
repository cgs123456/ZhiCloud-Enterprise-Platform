package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpPeriodCloseDO;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodCloseTypeEnum;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 期末处理记录 Mapper（P0-6）
 *
 * @author 智云
 */
@Mapper
public interface ErpPeriodCloseMapper extends BaseMapperX<ErpPeriodCloseDO> {

    default ErpPeriodCloseDO selectByPeriodAndType(Long periodId, Integer type) {
        return selectOne(new LambdaQueryWrapperX<ErpPeriodCloseDO>()
                .eq(ErpPeriodCloseDO::getPeriodId, periodId)
                .eq(ErpPeriodCloseDO::getType, type));
    }

    /**
     * 检查指定期间是否已成功执行某类期末处理
     */
    default boolean existsSuccess(Long periodId, ErpPeriodCloseTypeEnum typeEnum) {
        Long count = selectCount(new LambdaQueryWrapperX<ErpPeriodCloseDO>()
                .eq(ErpPeriodCloseDO::getPeriodId, periodId)
                .eq(ErpPeriodCloseDO::getType, typeEnum.getType()));
        return count != null && count > 0;
    }

}
