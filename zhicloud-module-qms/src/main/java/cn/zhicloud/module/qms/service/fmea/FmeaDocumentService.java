package cn.zhicloud.module.qms.service.fmea;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.fmea.vo.FmeaDocumentPageReqVO;
import cn.zhicloud.module.qms.controller.admin.fmea.vo.FmeaDocumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.fmea.FmeaDocumentDO;
import cn.zhicloud.module.qms.enums.qms.FmeaActionPriorityEnum;
import jakarta.validation.Valid;

/**
 * QMS FMEA 文档 Service 接口
 *
 * @author 智云
 */
public interface FmeaDocumentService {

    /**
     * 创建 FMEA 文档
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFmeaDocument(@Valid FmeaDocumentSaveReqVO createReqVO);

    /**
     * 更新 FMEA 文档
     *
     * @param updateReqVO 更新信息
     */
    void updateFmeaDocument(@Valid FmeaDocumentSaveReqVO updateReqVO);

    /**
     * 删除 FMEA 文档
     *
     * @param id 编号
     */
    void deleteFmeaDocument(Long id);

    /**
     * 获得 FMEA 文档
     *
     * @param id 编号
     * @return FMEA 文档
     */
    FmeaDocumentDO getFmeaDocument(Long id);

    /**
     * 获得 FMEA 文档分页
     *
     * @param pageReqVO 分页查询
     * @return FMEA 文档分页
     */
    PageResult<FmeaDocumentDO> getFmeaDocumentPage(FmeaDocumentPageReqVO pageReqVO);

    /**
     * 计算 AIAG-VDA 2019 行动优先级（Action Priority）
     *
     * <p>基于 S（严重度）/ O（频度）/ D（探测度）三维组合查表，
     * 返回 HIGH / MEDIUM / LOW 三级行动优先级，替代传统 RPN 阈值判定。
     *
     * @param severity  严重度 S（1-10）
     * @param occurrence 频度 O（1-10）
     * @param detection  探测度 D（1-10）
     * @return 行动优先级枚举
     */
    FmeaActionPriorityEnum calculateActionPriority(int severity, int occurrence, int detection);

    /**
     * 计算 RPN（Risk Priority Number 风险优先数）
     *
     * <p>旧版 FMEA 风险评估指标：RPN = S × O × D（取值范围 1-1000）。
     *
     * @param severity  严重度 S（1-10）
     * @param occurrence 频度 O（1-10）
     * @param detection  探测度 D（1-10）
     * @return RPN 风险优先数
     */
    int calculateRpn(int severity, int occurrence, int detection);

}