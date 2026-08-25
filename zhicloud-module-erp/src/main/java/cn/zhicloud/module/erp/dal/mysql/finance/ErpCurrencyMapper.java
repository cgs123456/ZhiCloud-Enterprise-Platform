package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencyPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCurrencyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 币种 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpCurrencyMapper extends BaseMapperX<ErpCurrencyDO> {

    default ErpCurrencyDO selectByCode(String code) {
        return selectOne(ErpCurrencyDO::getCode, code);
    }

    default ErpCurrencyDO selectByBase() {
        return selectOne(ErpCurrencyDO::getIsBase, true);
    }

    default List<ErpCurrencyDO> selectListByEnabled(Integer enabled) {
        return selectList(ErpCurrencyDO::getEnabled, enabled);
    }

    default PageResult<ErpCurrencyDO> selectPage(ErpCurrencyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCurrencyDO>()
                .likeIfPresent(ErpCurrencyDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpCurrencyDO::getName, reqVO.getName())
                .eqIfPresent(ErpCurrencyDO::getEnabled, reqVO.getEnabled())
                .eqIfPresent(ErpCurrencyDO::getIsBase, reqVO.getIsBase())
                .orderByDesc(ErpCurrencyDO::getId));
    }

}
