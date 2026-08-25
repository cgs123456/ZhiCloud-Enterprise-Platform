package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costcenter.ErpCostCenterPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCostCenterDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 成本中心 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpCostCenterMapper extends BaseMapperX<ErpCostCenterDO> {

    default ErpCostCenterDO selectByCode(String code) {
        return selectOne(ErpCostCenterDO::getCode, code);
    }

    default List<ErpCostCenterDO> selectListByParentId(Long parentId) {
        return selectList(ErpCostCenterDO::getParentId, parentId);
    }

    default PageResult<ErpCostCenterDO> selectPage(ErpCostCenterPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCostCenterDO>()
                .likeIfPresent(ErpCostCenterDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpCostCenterDO::getName, reqVO.getName())
                .eqIfPresent(ErpCostCenterDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpCostCenterDO::getCode));
    }

}
