package cn.zhicloud.module.wms.service.order.dock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.order.dock.vo.WmsDockPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.dock.vo.WmsDockSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.dock.WmsDockDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Map;

/**
 * WMS 月台 Service 接口
 *
 * @author 智云
 */
public interface WmsDockService {

    /**
     * 创建月台
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDock(@Valid WmsDockSaveReqVO createReqVO);

    /**
     * 更新月台
     *
     * @param updateReqVO 更新信息
     */
    void updateDock(@Valid WmsDockSaveReqVO updateReqVO);

    /**
     * 删除月台
     *
     * @param id 编号
     */
    void deleteDock(Long id);

    /**
     * 获得月台
     *
     * @param id 编号
     * @return 月台
     */
    WmsDockDO getDock(Long id);

    /**
     * 获得月台分页
     *
     * @param pageReqVO 分页查询
     * @return 月台分页
     */
    PageResult<WmsDockDO> getDockPage(WmsDockPageReqVO pageReqVO);

    /**
     * 校验月台存在
     *
     * @param id 编号
     * @return 月台
     */
    WmsDockDO validateDockExists(Long id);

    /**
     * 批量获取月台 Map
     *
     * @param ids 编号集合
     * @return 月台 Map
     */
    Map<Long, WmsDockDO> getDockMap(Collection<Long> ids);

}
