package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitcenter.ErpProfitCenterPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpProfitCenterDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 利润中心 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpProfitCenterMapper extends BaseMapperX<ErpProfitCenterDO> {

    default ErpProfitCenterDO selectByCode(String code) {
        return selectOne(ErpProfitCenterDO::getCode, code);
    }

    default List<ErpProfitCenterDO> selectListByParentId(Long parentId) {
        return selectList(ErpProfitCenterDO::getParentId, parentId);
    }

    default PageResult<ErpProfitCenterDO> selectPage(ErpProfitCenterPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpProfitCenterDO>()
                .likeIfPresent(ErpProfitCenterDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpProfitCenterDO::getName, reqVO.getName())
                .eqIfPresent(ErpProfitCenterDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpProfitCenterDO::getCode));
    }

}
