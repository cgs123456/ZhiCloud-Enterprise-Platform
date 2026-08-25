package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 会计科目 Mapper（P0-7）
 *
 * @author 智云
 */
@Mapper
public interface ErpGlAccountMapper extends BaseMapperX<ErpGlAccountDO> {

    default ErpGlAccountDO selectByCode(String code) {
        return selectOne(ErpGlAccountDO::getCode, code);
    }

    default List<ErpGlAccountDO> selectListByParentId(Long parentId) {
        return selectList(ErpGlAccountDO::getParentId, parentId);
    }

    default List<ErpGlAccountDO> selectListByLeaf(Boolean isLeaf) {
        return selectList(ErpGlAccountDO::getIsLeaf, isLeaf);
    }

    default PageResult<ErpGlAccountDO> selectPage(ErpGlAccountPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpGlAccountDO>()
                .likeIfPresent(ErpGlAccountDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpGlAccountDO::getName, reqVO.getName())
                .eqIfPresent(ErpGlAccountDO::getType, reqVO.getType())
                .eqIfPresent(ErpGlAccountDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpGlAccountDO::getIsLeaf, reqVO.getIsLeaf())
                .orderByAsc(ErpGlAccountDO::getCode));
    }

}
