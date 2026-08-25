package cn.zhicloud.module.mes.service.pro.piecework;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRecordPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRecordDO;

import java.util.List;

/**
 * MES 计件工资明细 Service 接口
 *
 * @author 智云
 */
public interface MesProPieceworkRecordService {

    /**
     * 根据报工单生成计件工资明细（报工审批通过时调用）
     *
     * <p>匹配 {@link cn.zhicloud.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRuleDO}，
     * 计算合格品 / 废品工资并落库。同一报工单重复调用会校验拦截。
     *
     * @param feedbackId 报工单编号
     * @return 生成的计件明细编号；未匹配到规则时返回 null
     */
    Long generateRecordFromFeedback(Long feedbackId);

    /**
     * 获得计件工资明细
     *
     * @param id 编号
     * @return 计件工资明细
     */
    MesProPieceworkRecordDO getPieceworkRecord(Long id);

    /**
     * 获得计件工资明细分页
     *
     * @param pageReqVO 分页查询
     * @return 计件工资明细分页
     */
    PageResult<MesProPieceworkRecordDO> getPieceworkRecordPage(MesProPieceworkRecordPageReqVO pageReqVO);

    /**
     * 按月份查询所有正常状态的计件明细（供月度汇总使用）
     *
     * @param periodMonth 月份（yyyyMM）
     * @return 计件明细列表
     */
    List<MesProPieceworkRecordDO> getPieceworkRecordListByPeriod(String periodMonth);

}
