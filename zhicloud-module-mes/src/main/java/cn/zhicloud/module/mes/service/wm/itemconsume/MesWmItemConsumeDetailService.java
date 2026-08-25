package cn.zhicloud.module.mes.service.wm.itemconsume;

import cn.zhicloud.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeDetailDO;

import java.util.List;

/**
 * MES 物料消耗记录明细 Service 接口
 *
 * @author 智云
 */
public interface MesWmItemConsumeDetailService {

    /**
     * 批量创建消耗明细
     *
     * @param details 消耗明细列表
     */
    void createItemConsumeDetailBatch(List<MesWmItemConsumeDetailDO> details);

    /**
     * 查询指定消耗记录的所有明细
     *
     * @param consumeId 消耗记录编号
     * @return 消耗明细列表
     */
    List<MesWmItemConsumeDetailDO> getItemConsumeDetailListByConsumeId(Long consumeId);

}
