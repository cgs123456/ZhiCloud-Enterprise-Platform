package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 凭证模板 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsVoucherTemplateMapper extends BaseMapperX<FmsVoucherTemplateDO> {

    default List<FmsVoucherTemplateDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsVoucherTemplateDO>()
                .eq(FmsVoucherTemplateDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsVoucherTemplateDO::getId));
    }

    default FmsVoucherTemplateDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsVoucherTemplateDO>()
                .eq(FmsVoucherTemplateDO::getAccountSetId, accountSetId)
                .eq(FmsVoucherTemplateDO::getId, id));
    }

    default FmsVoucherTemplateDO selectByAccountSetAndUnique(Long accountSetId, String value) {
        return selectOne(new LambdaQueryWrapperX<FmsVoucherTemplateDO>()
                .eq(FmsVoucherTemplateDO::getAccountSetId, accountSetId)
                .eq(FmsVoucherTemplateDO::getName, value));
    }

}
