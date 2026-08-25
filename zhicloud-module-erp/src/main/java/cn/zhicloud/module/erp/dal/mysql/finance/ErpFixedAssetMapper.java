package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 固定资产 Mapper（P0-14）
 *
 * @author 智云
 */
@Mapper
public interface ErpFixedAssetMapper extends BaseMapperX<ErpFixedAssetDO> {

    default ErpFixedAssetDO selectByCode(String code) {
        return selectOne(ErpFixedAssetDO::getCode, code);
    }

    default List<ErpFixedAssetDO> selectListByStatus(Integer status) {
        return selectList(ErpFixedAssetDO::getStatus, status);
    }

    default PageResult<ErpFixedAssetDO> selectPage(ErpFixedAssetPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpFixedAssetDO>()
                .likeIfPresent(ErpFixedAssetDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpFixedAssetDO::getName, reqVO.getName())
                .eqIfPresent(ErpFixedAssetDO::getCategory, reqVO.getCategory())
                .eqIfPresent(ErpFixedAssetDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpFixedAssetDO::getDepartmentId, reqVO.getDepartmentId())
                .orderByDesc(ErpFixedAssetDO::getId));
    }

}
