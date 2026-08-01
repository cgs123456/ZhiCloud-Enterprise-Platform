package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 会计期间 Mapper（P0-6）
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPeriodMapper extends BaseMapperX<ErpPeriodDO> {

    default ErpPeriodDO selectByCode(String code) {
        return selectOne(ErpPeriodDO::getCode, code);
    }

    default List<ErpPeriodDO> selectListByYear(Integer year) {
        return selectList(ErpPeriodDO::getYear, year);
    }

}
