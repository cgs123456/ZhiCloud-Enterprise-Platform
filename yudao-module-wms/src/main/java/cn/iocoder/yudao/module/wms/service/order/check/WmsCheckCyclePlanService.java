package cn.iocoder.yudao.module.wms.service.order.check;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.check.WmsCheckCyclePlanDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * WMS 循环盘点计划 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsCheckCyclePlanService {

    /**
     * 创建循环盘点计划
     */
    Long createCheckCyclePlan(@Valid WmsCheckCyclePlanSaveReqVO createReqVO);

    /**
     * 更新循环盘点计划
     */
    void updateCheckCyclePlan(@Valid WmsCheckCyclePlanSaveReqVO updateReqVO);

    /**
     * 删除循环盘点计划
     */
    void deleteCheckCyclePlan(Long id);

    /**
     * 获得循环盘点计划
     */
    WmsCheckCyclePlanDO getCheckCyclePlan(Long id);

    /**
     * 获得循环盘点计划分页
     */
    PageResult<WmsCheckCyclePlanDO> getCheckCyclePlanPage(WmsCheckCyclePlanPageReqVO pageReqVO);

    /**
     * 获得到期需盘点的计划列表（Job 使用）
     *
     * @param today 当前日期
     * @return 计划列表
     */
    List<WmsCheckCyclePlanDO> getDueCheckCyclePlanList(LocalDate today);

    /**
     * 获得全部启用的计划列表
     */
    List<WmsCheckCyclePlanDO> getEnabledCheckCyclePlanList();

    /**
     * 更新下次盘点日期（Job 回写）
     */
    void updateNextCheckDate(Long id, LocalDate nextCheckDate);

    /**
     * 校验循环盘点计划存在
     */
    WmsCheckCyclePlanDO validateCheckCyclePlanExists(Long id);

}