package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodCloseDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPeriodCloseTypeEnum;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 期末处理记录 Mapper（P0-6）
 *
 * @author 芋道源码
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
