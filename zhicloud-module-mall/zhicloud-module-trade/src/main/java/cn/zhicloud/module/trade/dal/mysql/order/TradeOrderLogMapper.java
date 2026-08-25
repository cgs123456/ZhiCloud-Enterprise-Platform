package cn.zhicloud.module.trade.dal.mysql.order;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.trade.dal.dataobject.order.TradeOrderLogDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeOrderLogMapper extends BaseMapperX<TradeOrderLogDO> {

    default List<TradeOrderLogDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<TradeOrderLogDO>()
                .eq(TradeOrderLogDO::getOrderId, orderId)
                .orderByDesc(TradeOrderLogDO::getCreateTime));
    }

}
