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

    /**
     * 按编号加行锁查询科目（用于余额读-改-写，防止并发审核丢更新）
     *
     * <p>§12.226：凭证审核/反审核维护科目累计发生额与期末余额，是典型的
     * read-modify-write。此前用 selectById 读、内存累加、updateById 全量覆盖——
     * 并发审核两张命中同一科目的凭证时，后写会覆盖先写，导致金额少计。
     * 加 FOR UPDATE 行锁后同一科目的余额维护串行化。必须在事务内调用。
     */
    default ErpGlAccountDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpGlAccountDO>()
                .eq(ErpGlAccountDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<ErpGlAccountDO> selectListByParentId(Long parentId) {
        return selectList(ErpGlAccountDO::getParentId, parentId);
    }

    default List<ErpGlAccountDO> selectListByLeaf(Boolean isLeaf) {
        return selectList(ErpGlAccountDO::getIsLeaf, isLeaf);
    }

    /**
     * 按科目类型查询（期初余额页按类别取该类别全树，按编码升序；C6③ / ADR 265）
     */
    default List<ErpGlAccountDO> selectListByType(Integer type) {
        return selectList(new LambdaQueryWrapperX<ErpGlAccountDO>()
                .eq(ErpGlAccountDO::getType, type)
                .orderByAsc(ErpGlAccountDO::getCode));
    }

    /**
     * 查询全部末级科目（按编码升序；期初余额导出 / 导入模板，C6③ / ADR 265）
     */
    default List<ErpGlAccountDO> selectListByLeafOrderByCode() {
        return selectList(new LambdaQueryWrapperX<ErpGlAccountDO>()
                .eq(ErpGlAccountDO::getIsLeaf, true)
                .orderByAsc(ErpGlAccountDO::getCode));
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
