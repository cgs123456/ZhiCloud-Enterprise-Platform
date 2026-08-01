package cn.iocoder.yudao.module.mes.service.pro.piecework;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRuleDO;
import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * MES 计件工资规则 Service 接口
 *
 * @author 芋道源码
 */
public interface MesProPieceworkRuleService {

    /**
     * 创建计件工资规则
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPieceworkRule(@Valid MesProPieceworkRuleSaveReqVO createReqVO);

    /**
     * 更新计件工资规则
     *
     * @param updateReqVO 更新信息
     */
    void updatePieceworkRule(@Valid MesProPieceworkRuleSaveReqVO updateReqVO);

    /**
     * 删除计件工资规则
     *
     * @param id 编号
     */
    void deletePieceworkRule(Long id);

    /**
     * 获得计件工资规则
     *
     * @param id 编号
     * @return 计件工资规则
     */
    MesProPieceworkRuleDO getPieceworkRule(Long id);

    /**
     * 获得计件工资规则分页
     *
     * @param pageReqVO 分页查询
     * @return 计件工资规则分页
     */
    PageResult<MesProPieceworkRuleDO> getPieceworkRulePage(MesProPieceworkRulePageReqVO pageReqVO);

    /**
     * 按工序 / 产品 / 工作站 / 工艺路线匹配生效中的计件规则
     *
     * <p>匹配优先级（由精到粗）：
     * 工作站+工序+产品 &gt; 工序+产品 &gt; 工序 &gt; 产品。
     * 同维度多条命中时取最近一条（按 id 倒序）。
     *
     * @param processId 工序编号（可空）
     * @param itemId 产品物料编号（可空）
     * @param workstationId 工作站编号（可空）
     * @param routeId 工艺路线编号（可空）
     * @param effectDate 生效日期（用于校验生效区间，可空则取当天）
     * @return 匹配到的规则；未匹配返回 null
     */
    MesProPieceworkRuleDO matchRule(Long processId, Long itemId, Long workstationId, Long routeId, LocalDate effectDate);

}
