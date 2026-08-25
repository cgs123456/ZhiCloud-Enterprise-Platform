package cn.zhicloud.module.wms.service.pda;

import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaCheckReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaLoginReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaLoginRespVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaPickReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaPutawayReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaReceiptReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaScanReqVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaScanRespVO;
import cn.zhicloud.module.wms.controller.app.pda.vo.WmsPdaTaskRespVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * WMS PDA 移动端 Service 接口
 *
 * @author 智云
 */
public interface WmsPdaService {

    /**
     * PDA 设备登录
     *
     * @param reqVO 登录请求
     * @return 登录响应
     */
    WmsPdaLoginRespVO login(@Valid WmsPdaLoginReqVO reqVO);

    /**
     * 获取待执行任务列表（拣货/上架/盘点）
     *
     * @return 任务列表
     */
    List<WmsPdaTaskRespVO> getTaskList();

    /**
     * 扫码（库位/物料）
     *
     * @param reqVO 扫码请求
     * @return 扫码响应
     */
    WmsPdaScanRespVO scan(@Valid WmsPdaScanReqVO reqVO);

    /**
     * PDA 收货确认
     *
     * @param reqVO 收货确认请求
     */
    void confirmReceipt(@Valid WmsPdaReceiptReqVO reqVO);

    /**
     * PDA 上架执行
     *
     * @param reqVO 上架执行请求
     */
    void executePutaway(@Valid WmsPdaPutawayReqVO reqVO);

    /**
     * PDA 拣货执行
     *
     * @param reqVO 拣货执行请求
     */
    void executePick(@Valid WmsPdaPickReqVO reqVO);

    /**
     * PDA 盘点录入
     *
     * @param reqVO 盘点录入请求
     */
    void executeCheck(@Valid WmsPdaCheckReqVO reqVO);

}
