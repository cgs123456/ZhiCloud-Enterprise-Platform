package cn.iocoder.yudao.module.mes.service.md.bom.substitute;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstitutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo.MesBomSubstituteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.bom.MesBomSubstituteDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * MES BOM 替代料 Service 接口
 *
 * @author 芋道源码
 */
public interface MesBomSubstituteService {

    /**
     * 创建 BOM 替代料
     */
    Long createBomSubstitute(@Valid MesBomSubstituteSaveReqVO createReqVO);

    /**
     * 更新 BOM 替代料
     */
    void updateBomSubstitute(@Valid MesBomSubstituteSaveReqVO updateReqVO);

    /**
     * 删除 BOM 替代料
     */
    void deleteBomSubstitute(Long id);

    /**
     * 校验 BOM 替代料存在
     */
    MesBomSubstituteDO validateBomSubstituteExists(Long id);

    /**
     * 获得 BOM 替代料
     */
    MesBomSubstituteDO getBomSubstitute(Long id);

    /**
     * 获得 BOM 替代料分页
     */
    PageResult<MesBomSubstituteDO> getBomSubstitutePage(MesBomSubstitutePageReqVO pageReqVO);

    /**
     * 按 BOM 明细 ID 获取替代料列表（按 priority 升序，首选在前）
     *
     * 用于 BOM 展算缺料时按优先级选取替代料
     */
    List<MesBomSubstituteDO> getSubstitutesByBomDetailId(Long bomDetailId);

    /**
     * 按 BOM 主表 ID 删除全部替代料（BOM 主数据删除时级联调用）
     */
    void deleteBomSubstituteByBomId(Long bomId);

}