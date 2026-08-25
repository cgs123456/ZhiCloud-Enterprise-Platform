package cn.zhicloud.module.qms.service.msa;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.msa.vo.*;
import cn.zhicloud.module.qms.dal.dataobject.msa.MsaMeasurementDO;
import cn.zhicloud.module.qms.dal.dataobject.msa.MsaStudyDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS MSA 研究 Service 接口
 *
 * @author 智云
 */
public interface MsaStudyService {

    /**
     * 创建 MSA 研究
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMsaStudy(@Valid MsaStudySaveReqVO createReqVO);

    /**
     * 更新 MSA 研究
     *
     * @param updateReqVO 更新信息
     */
    void updateMsaStudy(@Valid MsaStudySaveReqVO updateReqVO);

    /**
     * 删除 MSA 研究
     *
     * @param id 编号
     */
    void deleteMsaStudy(Long id);

    /**
     * 获得 MSA 研究
     *
     * @param id 编号
     * @return MSA 研究
     */
    MsaStudyDO getMsaStudy(Long id);

    /**
     * 获得 MSA 研究分页
     *
     * @param pageReqVO 分页查询
     * @return MSA 研究分页
     */
    PageResult<MsaStudyDO> getMsaStudyPage(MsaStudyPageReqVO pageReqVO);

    /**
     * 保存/录入测量数据
     *
     * @param reqVO 测量数据
     * @return 编号
     */
    Long saveMeasurement(@Valid MsaMeasurementSaveReqVO reqVO);

    /**
     * 获取测量数据
     *
     * @param studyId 研究 ID
     * @return 测量数据列表
     */
    List<MsaMeasurementDO> getMeasurementData(Long studyId);

    /**
     * 计算 GR&R（均值极差法 Xbar-R）
     *
     * <p>评价准则：
     * <ul>
     *   <li>%GR&R &lt; 10%：可接受</li>
     *   <li>10% &le; %GR&R &le; 30%：有条件接受</li>
     *   <li>%GR&R &gt; 30%：不可接受</li>
     * </ul>
     *
     * @param studyId 研究 ID
     * @return GR&R 分析结果
     */
    MsaGageRRRespVO calculateGageRR(Long studyId);

}
