package cn.zhicloud.module.iot.gateway.protocol.tcp.handler.downstream;

import cn.zhicloud.module.iot.core.messagebus.core.IotMessageBus;
import cn.zhicloud.module.iot.core.mq.message.IotDeviceMessage;
import cn.zhicloud.module.iot.gateway.protocol.IotProtocol;
import cn.zhicloud.module.iot.gateway.protocol.AbstractIotProtocolDownstreamSubscriber;
import lombok.extern.slf4j.Slf4j;

/**
 * IoT 网关 TCP 下游订阅者：接收下行给设备的消息
 *
 * @author 智云
 */
@Slf4j
public class IotTcpDownstreamSubscriber extends AbstractIotProtocolDownstreamSubscriber {

    private final IotTcpDownstreamHandler downstreamHandler;

    public IotTcpDownstreamSubscriber(IotProtocol protocol,
                                      IotTcpDownstreamHandler downstreamHandler,
                                      IotMessageBus messageBus) {
        super(protocol, messageBus);
        this.downstreamHandler = downstreamHandler;
    }

    @Override
    protected void handleMessage(IotDeviceMessage message) {
        downstreamHandler.handle(message);
    }

}
