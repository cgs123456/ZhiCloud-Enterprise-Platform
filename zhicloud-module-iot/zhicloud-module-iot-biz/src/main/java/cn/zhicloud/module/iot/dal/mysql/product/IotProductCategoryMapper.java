package cn.zhicloud.module.iot.dal.mysql.product;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.iot.controller.admin.product.vo.category.IotProductCategoryPageReqVO;
import cn.zhicloud.module.iot.dal.dataobject.product.IotProductCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * IoT 产品分类 Mapper
 *
 * @author 智云
 */
@Mapper
public interface IotProductCategoryMapper extends BaseMapperX<IotProductCategoryDO> {

    default PageResult<IotProductCategoryDO> selectPage(IotProductCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IotProductCategoryDO>()
                .likeIfPresent(IotProductCategoryDO::getName, reqVO.getName())
                .betweenIfPresent(IotProductCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(IotProductCategoryDO::getSort));
    }

    default List<IotProductCategoryDO> selectListByStatus(Integer status) {
        return selectList(IotProductCategoryDO::getStatus, status);
    }

    default Long selectCountByCreateTime(@Nullable LocalDateTime createTime) {
        return selectCount(new LambdaQueryWrapperX<IotProductCategoryDO>()
                .geIfPresent(IotProductCategoryDO::getCreateTime, createTime));
    }

}