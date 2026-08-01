package cn.iocoder.yudao.module.mes.dal.mysql.pro.piecework;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRulePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * MES 计件工资规则 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProPieceworkRuleMapper extends BaseMapperX<MesProPieceworkRuleDO> {

    default PageResult<MesProPieceworkRuleDO> selectPage(MesProPieceworkRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProPieceworkRuleDO>()
                .likeIfPresent(MesProPieceworkRuleDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(MesProPieceworkRuleDO::getProcessId, reqVO.getProcessId())
                .eqIfPresent(MesProPieceworkRuleDO::getRouteId, reqVO.getRouteId())
                .eqIfPresent(MesProPieceworkRuleDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesProPieceworkRuleDO::getWorkstationId, reqVO.getWorkstationId())
                .eqIfPresent(MesProPieceworkRuleDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProPieceworkRuleDO::getEnabled, reqVO.getEnabled())
                .orderByDesc(MesProPieceworkRuleDO::getId));
    }

    /**
     * 查询在指定日期生效的启用规则（按工序 / 产品 / 工作站维度动态匹配）
     */
    default List<MesProPieceworkRuleDO> selectEnabledEffectiveRules(LocalDate effectDate) {
        return selectList(new LambdaQueryWrapperX<MesProPieceworkRuleDO>()
                .eq(MesProPieceworkRuleDO::getEnabled, 0) // 0=启用
                .le(MesProPieceworkRuleDO::getEffectiveDate, effectDate)
                .ge(MesProPieceworkRuleDO::getExpireDate, effectDate));
    }

}
