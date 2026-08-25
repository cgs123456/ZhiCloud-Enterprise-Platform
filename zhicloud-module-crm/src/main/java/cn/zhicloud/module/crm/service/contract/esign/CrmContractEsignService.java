package cn.zhicloud.module.crm.service.contract.esign;

import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmContractEsignRespVO;
import cn.zhicloud.module.crm.controller.admin.contract.vo.esign.CrmEsignCallbackReqVO;

/**
 * CRM 合同电子签 Service 接口
 *
 * 对接法大大/e签宝等第三方电子签平台（当前为 stub 模式）
 *
 * @author dhb52
 */
public interface CrmContractEsignService {

    /**
     * 发起电子签
     *
     * @param contractId 合同编号
     * @return 电子签任务 ID
     */
    String initEsign(Long contractId);

    /**
     * 查询签署状态
     *
     * @param esignTaskId 电子签任务 ID
     * @return 签署状态信息
     */
    CrmContractEsignRespVO getEsignStatus(String esignTaskId);

    /**
     * 下载已签署合同
     *
     * @param contractId 合同编号
     * @return 已签署合同 URL 列表
     */
    CrmContractEsignRespVO downloadSignedContract(Long contractId);

    /**
     * 处理电子签回调
     *
     * @param req 回调请求
     */
    void handleEsignCallback(CrmEsignCallbackReqVO req);

}
