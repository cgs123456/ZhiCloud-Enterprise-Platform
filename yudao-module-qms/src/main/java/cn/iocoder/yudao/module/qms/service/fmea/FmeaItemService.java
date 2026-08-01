package cn.iocoder.yudao.module.qms.service.fmea;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.fmea.FmeaItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS FMEA 条目 Service 接口
 *
 * @author 芋道源码
 */
public interface FmeaItemService {

    /**
     * 创建 FMEA 条目
     *
     * <p>RPN = S * O * D 在 Service 层自动计算，并判定风险等级。
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFmeaItem(@Valid FmeaItemSaveReqVO createReqVO);

    /**
     * 更新 FMEA 条目
     *
     * <p>更新时自动重算 RPN 与风险等级。
     *
     * @param updateReqVO 更新信息
     */
    void updateFmeaItem(@Valid FmeaItemSaveReqVO updateReqVO);

    /**
     * 删除 FMEA 条目
     *
     * @param id 编号
     */
    void deleteFmeaItem(Long id);

    /**
     * 获得 FMEA 条目
     *
     * @param id 编号
     * @return FMEA 条目
     */
    FmeaItemDO getFmeaItem(Long id);

    /**
     * 获得 FMEA 条目分页
     *
     * @param pageReqVO 分页查询
     * @return FMEA 条目分页
     */
    PageResult<FmeaItemDO> getFmeaItemPage(FmeaItemPageReqVO pageReqVO);

    /**
     * 获得 FMEA 文档下的全部条目
     *
     * @param fmeaId FMEA 文档 ID
     * @return 条目列表
     */
    List<FmeaItemDO> getFmeaItemListByFmeaId(Long fmeaId);

}
