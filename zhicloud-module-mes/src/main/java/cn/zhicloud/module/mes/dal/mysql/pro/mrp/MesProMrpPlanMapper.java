package cn.zhicloud.module.mes.dal.mysql.pro.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpPlanDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

/**
 * MES MRP 计划 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesProMrpPlanMapper extends BaseMapperX<MesProMrpPlanDO> {

    default PageResult<MesProMrpPlanDO> selectPage(MesProMrpPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProMrpPlanDO>()
                .likeIfPresent(MesProMrpPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(MesProMrpPlanDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProMrpPlanDO::getPlanDate, reqVO.getPlanDate())
                .orderByDesc(MesProMrpPlanDO::getId));
    }

    default MesProMrpPlanDO selectByPlanNo(String planNo) {
        return selectOne(MesProMrpPlanDO::getPlanNo, planNo);
    }

    /**
     * 状态流转条件更新（CAS）：仅当当前状态在 fromStatuses 中时才更新，并发下只有一个动作能生效
     *
     * @param id 计划编号
     * @param fromStatuses 允许的来源状态集合
     * @param updateObj 更新内容（需携带 id）
     * @return 影响行数（0 表示来源状态不匹配或记录不存在）
     */
    default int updateByIdAndStatus(Long id, Collection<Integer> fromStatuses, MesProMrpPlanDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<MesProMrpPlanDO>()
                .eq(MesProMrpPlanDO::getId, id)
                .in(MesProMrpPlanDO::getStatus, fromStatuses));
    }

}
