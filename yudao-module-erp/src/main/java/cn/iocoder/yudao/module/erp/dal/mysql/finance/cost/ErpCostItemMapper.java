package cn.iocoder.yudao.module.erp.dal.mysql.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem.ErpCostItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpCostItemMapper extends BaseMapperX<ErpCostItemDO> {

    default ErpCostItemDO selectByCode(String code) {
        return selectOne(ErpCostItemDO::getCode, code);
    }

    default PageResult<ErpCostItemDO> selectPage(ErpCostItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCostItemDO>()
                .likeIfPresent(ErpCostItemDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpCostItemDO::getName, reqVO.getName())
                .eqIfPresent(ErpCostItemDO::getType, reqVO.getType())
                .eqIfPresent(ErpCostItemDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpCostItemDO::getSort));
    }

    default List<ErpCostItemDO> selectListByStatus(Integer status) {
        return selectList(ErpCostItemDO::getStatus, status);
    }

}
