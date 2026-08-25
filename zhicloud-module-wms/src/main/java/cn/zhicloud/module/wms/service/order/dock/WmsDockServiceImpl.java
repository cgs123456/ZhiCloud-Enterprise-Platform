package cn.zhicloud.module.wms.service.order.dock;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.order.dock.vo.WmsDockPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.dock.vo.WmsDockSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.dock.WmsDockDO;
import cn.zhicloud.module.wms.dal.mysql.order.dock.WmsDockMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.DOCK_HAS_ASN;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.DOCK_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.DOCK_CODE_DUPLICATE;

/**
 * WMS 月台 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsDockServiceImpl implements WmsDockService {

    /**
     * 月台状态：空闲
     */
    private static final int STATUS_IDLE = 10;

    @Resource
    private WmsDockMapper dockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDock(WmsDockSaveReqVO createReqVO) {
        // 1. 校验月台编号唯一
        validateDockCodeUnique(null, createReqVO.getDockCode());
        // 2. 插入月台
        WmsDockDO dock = BeanUtils.toBean(createReqVO, WmsDockDO.class);
        if (dock.getStatus() == null) {
            dock.setStatus(STATUS_IDLE);
        }
        dockMapper.insert(dock);
        return dock.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDock(WmsDockSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateDockExists(updateReqVO.getId());
        // 2. 校验月台编号唯一
        validateDockCodeUnique(updateReqVO.getId(), updateReqVO.getDockCode());
        // 3. 更新
        WmsDockDO updateObj = BeanUtils.toBean(updateReqVO, WmsDockDO.class);
        dockMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDock(Long id) {
        // 1. 校验存在
        validateDockExists(id);
        // 2. 删除
        dockMapper.deleteById(id);
    }

    @Override
    public WmsDockDO getDock(Long id) {
        return dockMapper.selectById(id);
    }

    @Override
    public PageResult<WmsDockDO> getDockPage(WmsDockPageReqVO pageReqVO) {
        return dockMapper.selectPage(pageReqVO);
    }

    @Override
    public WmsDockDO validateDockExists(Long id) {
        if (id == null) {
            return null;
        }
        WmsDockDO dock = dockMapper.selectById(id);
        if (dock == null) {
            throw exception(DOCK_NOT_EXISTS);
        }
        return dock;
    }

    @Override
    public Map<Long, WmsDockDO> getDockMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return convertMap(dockMapper.selectBatchIds(ids), WmsDockDO::getId);
    }

    private void validateDockCodeUnique(Long id, String dockCode) {
        WmsDockDO dock = dockMapper.selectByDockCode(dockCode);
        if (dock == null) {
            return;
        }
        if (id == null || !dock.getId().equals(id)) {
            throw exception(DOCK_CODE_DUPLICATE);
        }
    }

}
