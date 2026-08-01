package cn.iocoder.yudao.module.qms.service.qualitycost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QmsQualityCostSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QualityCostSummaryRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo.QualityCostTrendRespVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.qualitycost.QmsQualityCostDO;
import jakarta.validation.Valid;

/**
 * QMS 质量成本 Service 接口（PAIF 模型）
 *
 * @author yudao
 */
public interface QmsQualityCostService {

    /**
     * 创建质量成本记录
     *
     * <p>简化关联：仅在创建时允许通过 relatedId / relatedType 关联 8D/NCR/CAPA 业务记录。
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createQualityCost(@Valid QmsQualityCostSaveReqVO createReqVO);

    /**
     * 更新质量成本记录
     *
     * @param updateReqVO 更新信息
     */
    void updateQualityCost(@Valid QmsQualityCostSaveReqVO updateReqVO);

    /**
     * 删除质量成本记录
     *
     * @param id 编号
     */
    void deleteQualityCost(Long id);

    /**
     * 获得质量成本记录
     *
     * @param id 编号
     * @return 质量成本记录
     */
    QmsQualityCostDO getQualityCost(Long id);

    /**
     * 获得质量成本分页
     *
     * @param pageReqVO 分页查询
     * @return 质量成本分页
     */
    PageResult<QmsQualityCostDO> getQualityCostPage(QmsQualityCostPageReqVO pageReqVO);

    /**
     * 按 PAIF 四类汇总指定年月的质量成本
     *
     * @param year  年度
     * @param month 月份（1-12）
     * @return 四类成本汇总（金额 + 总计 + 占比）
     */
    QualityCostSummaryRespVO getQualityCostSummary(Integer year, Integer month);

    /**
     * 按月份趋势统计指定年度的质量成本
     *
     * @param year 年度
     * @return 月度趋势列表
     */
    QualityCostTrendRespVO getQualityCostTrend(Integer year);

    /**
     * 累计统计指定年度区间月份的质量成本
     *
     * @param year       年度
     * @param startMonth 起始月份（1-12）
     * @param endMonth   结束月份（1-12）
     * @return 四类成本累计汇总
     */
    QualityCostSummaryRespVO getCumulativeQualityCost(Integer year, Integer startMonth, Integer endMonth);

}