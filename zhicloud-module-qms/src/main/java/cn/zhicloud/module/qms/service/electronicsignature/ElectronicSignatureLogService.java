package cn.zhicloud.module.qms.service.electronicsignature;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.electronicsignature.vo.ElectronicSignatureLogPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.electronicsignature.ElectronicSignatureLogDO;

/**
 * QMS 电子签名记录 Service 接口
 *
 * @author 智云
 */
public interface ElectronicSignatureLogService {

    /**
     * 记录电子签名日志
     *
     * @param userId           用户 ID
     * @param signatureMeaning 签名含义
     * @param operationType    操作类型
     * @param operationContent 操作内容
     * @param ipAddress        IP 地址
     * @param remark           备注
     */
    void recordLog(Long userId, String signatureMeaning, String operationType,
                   String operationContent, String ipAddress, String remark);

    /**
     * 获得电子签名记录分页
     *
     * @param pageReqVO 分页查询
     * @return 电子签名记录分页
     */
    PageResult<ElectronicSignatureLogDO> getElectronicSignatureLogPage(ElectronicSignatureLogPageReqVO pageReqVO);

}
