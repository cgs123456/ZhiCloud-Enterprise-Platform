package cn.iocoder.yudao.module.tms.service.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightCalculateReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightCalculateRespVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.freight.TmsFreightDO;

import jakarta.validation.Valid;

/**
 * TMS 运费结算 Service 接口
 *
 * @author yudao
 */
public interface TmsFreightService {

    /**
     * 创建运费结算单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFreight(@Valid TmsFreightSaveReqVO createReqVO);

    /**
     * 更新运费结算单
     *
     * @param updateReqVO 更新信息
     */
    void updateFreight(@Valid TmsFreightSaveReqVO updateReqVO);

    /**
     * 删除运费结算单
     *
     * @param id 编号
     */
    void deleteFreight(Long id);

    /**
     * 获得运费结算单
     *
     * @param id 编号
     * @return 运费结算单
     */
    TmsFreightDO getFreight(Long id);

    /**
     * 获得运费结算单分页
     *
     * @param pageReqVO 分页查询
     * @return 运费结算单分页
     */
    PageResult<TmsFreightDO> getFreightPage(TmsFreightPageReqVO pageReqVO);

    /**
     * 根据运单信息自动计算运费
     *
     * @param calculateReqVO 计算请求
     * @return 计算结果
     */
    TmsFreightCalculateRespVO calculateFreight(@Valid TmsFreightCalculateReqVO calculateReqVO);

    /**
     * 审核运费结算单
     *
     * @param id     编号
     * @param pass   是否通过
     * @param reason 驳回原因（pass=false 时必填）
     */
    void auditFreight(Long id, Boolean pass, String reason);

    /**
     * 结算运费结算单（审核通过后执行结算）
     *
     * @param id 编号
     */
    void settleFreight(Long id);

}
