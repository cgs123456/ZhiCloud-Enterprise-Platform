package cn.zhicloud.module.mes.dal.mysql.wm.itemconsume;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 物料消耗记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesWmItemConsumeMapper extends BaseMapperX<MesWmItemConsumeDO> {

    default MesWmItemConsumeDO selectByFeedbackId(Long feedbackId) {
        return selectOne(MesWmItemConsumeDO::getFeedbackId, feedbackId);
    }

}
