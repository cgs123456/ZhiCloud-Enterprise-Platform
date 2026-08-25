package cn.zhicloud.module.qms.service.inspectionitem;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.inspectionitem.vo.InspectionItemPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionitem.vo.InspectionItemSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionitem.InspectionItemDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * QMS 检验项目 Service 接口
 *
 * @author 智云
 */
public interface InspectionItemService {

    /**
     * 创建检验项目
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInspectionItem(@Valid InspectionItemSaveReqVO createReqVO);

    /**
     * 更新检验项目
     *
     * @param updateReqVO 更新信息
     */
    void updateInspectionItem(@Valid InspectionItemSaveReqVO updateReqVO);

    /**
     * 删除检验项目
     *
     * @param id 编号
     */
    void deleteInspectionItem(Long id);

    /**
     * 获得检验项目
     *
     * @param id 编号
     * @return 检验项目
     */
    InspectionItemDO getInspectionItem(Long id);

    /**
     * 获得检验项目列表
     *
     * @param ids 编号数组
     * @return 检验项目列表
     */
    List<InspectionItemDO> getInspectionItemList(Collection<Long> ids);

    /**
     * 获得检验项目分页
     *
     * @param pageReqVO 分页查询
     * @return 检验项目分页
     */
    PageResult<InspectionItemDO> getInspectionItemPage(InspectionItemPageReqVO pageReqVO);

}
