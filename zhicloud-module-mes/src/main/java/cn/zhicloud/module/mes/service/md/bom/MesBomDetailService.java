package cn.zhicloud.module.mes.service.md.bom;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomDetailPageReqVO;
import cn.zhicloud.module.mes.controller.admin.md.bom.vo.MesBomDetailSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDetailDO;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * MES BOM 明细 Service 接口
 *
 * @author 智云
 */
public interface MesBomDetailService {

    /**
     * 创建 BOM 明细
     */
    Long createBomDetail(@Valid MesBomDetailSaveReqVO createReqVO);

    /**
     * 更新 BOM 明细
     */
    void updateBomDetail(@Valid MesBomDetailSaveReqVO updateReqVO);

    /**
     * 删除 BOM 明细
     */
    void deleteBomDetail(Long id);

    /**
     * 校验 BOM 明细存在
     */
    MesBomDetailDO validateBomDetailExists(Long id);

    /**
     * 获得明细分页
     */
    PageResult<MesBomDetailDO> getBomDetailPage(MesBomDetailPageReqVO pageReqVO);

    /**
     * 按 BOM 主数据编号获取明细列表
     */
    List<MesBomDetailDO> getBomDetailListByBomId(Long bomId);

    /**
     * 按 BOM 主数据编号集合批量获取明细
     */
    List<MesBomDetailDO> getBomDetailListByBomIds(Collection<Long> bomIds);

    /**
     * 保存 BOM 明细列表（用于 BOM 主数据创建/更新时整体替换）
     */
    void saveBomDetailList(Long bomId, List<MesBomDetailSaveReqVO> details);

    /**
     * 按 BOM 主数据编号删除全部明细
     */
    void deleteBomDetailByBomId(Long bomId);

}