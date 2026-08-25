package cn.zhicloud.module.erp.dal.mysql.purchase.inquiry;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 采购比价单明细行 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpPurchaseCompareLineMapper extends BaseMapperX<ErpPurchaseCompareLineDO> {

    default List<ErpPurchaseCompareLineDO> selectListByCompareId(Long compareId) {
        return selectList(ErpPurchaseCompareLineDO::getCompareId, compareId);
    }

    default int deleteByCompareId(Long compareId) {
        return delete(ErpPurchaseCompareLineDO::getCompareId, compareId);
    }

}
