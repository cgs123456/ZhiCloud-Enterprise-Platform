package cn.zhicloud.module.fms.dal.mysql.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherTemplateCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 凭证模板分类 Mapper
 *
 * @author 智云
 */
@Mapper
public interface FmsVoucherTemplateCategoryMapper extends BaseMapperX<FmsVoucherTemplateCategoryDO> {

    default List<FmsVoucherTemplateCategoryDO> selectListByAccountSet(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsVoucherTemplateCategoryDO>()
                .eq(FmsVoucherTemplateCategoryDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsVoucherTemplateCategoryDO::getId));
    }

    default FmsVoucherTemplateCategoryDO selectByAccountSetAndId(Long accountSetId, Long id) {
        return selectOne(new LambdaQueryWrapperX<FmsVoucherTemplateCategoryDO>()
                .eq(FmsVoucherTemplateCategoryDO::getAccountSetId, accountSetId)
                .eq(FmsVoucherTemplateCategoryDO::getId, id));
    }

    default FmsVoucherTemplateCategoryDO selectByAccountSetAndUnique(Long accountSetId, String value) {
        return selectOne(new LambdaQueryWrapperX<FmsVoucherTemplateCategoryDO>()
                .eq(FmsVoucherTemplateCategoryDO::getAccountSetId, accountSetId)
                .eq(FmsVoucherTemplateCategoryDO::getName, value));
    }

}
