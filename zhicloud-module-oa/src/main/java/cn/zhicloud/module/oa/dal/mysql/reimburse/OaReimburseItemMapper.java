package cn.zhicloud.module.oa.dal.mysql.reimburse;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OA 报销明细 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaReimburseItemMapper extends BaseMapperX<OaReimburseItemDO> {

    default List<OaReimburseItemDO> selectListByReimburseId(Long reimburseId) {
        return selectList(OaReimburseItemDO::getReimburseId, reimburseId);
    }

    default int deleteByReimburseId(Long reimburseId) {
        return delete(new LambdaQueryWrapperX<OaReimburseItemDO>()
                .eq(OaReimburseItemDO::getReimburseId, reimburseId));
    }

}
