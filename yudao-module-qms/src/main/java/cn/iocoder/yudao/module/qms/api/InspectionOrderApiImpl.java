package cn.iocoder.yudao.module.qms.api;

import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * QMS 检验单 API 实现
 *
 * @author 智云
 */
@Service
@Validated
public class InspectionOrderApiImpl implements InspectionOrderApi {

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Override
    public boolean isQualified(String bizType, Long bizId) {
        InspectionOrderDO order = inspectionOrderMapper.selectLatestByBiz(bizType, bizId);
        // fail-closed：无检验单或非「检验通过」状态，一律不得放行入库
        return order != null && InspectionOrderStatusEnum.PASSED.getStatus().equals(order.getStatus());
    }

}
