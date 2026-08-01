package cn.iocoder.yudao.module.qms.service.inspectionitem;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionitem.vo.InspectionItemPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionitem.vo.InspectionItemSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionitem.InspectionItemDO;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionitem.InspectionItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSPECTION_ITEM_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSPECTION_ITEM_NOT_EXISTS;

/**
 * QMS 检验项目 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class InspectionItemServiceImpl implements InspectionItemService {

    @Resource
    private InspectionItemMapper inspectionItemMapper;

    @Override
    public Long createInspectionItem(InspectionItemSaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 插入
        InspectionItemDO inspectionItem = BeanUtils.toBean(createReqVO, InspectionItemDO.class);
        inspectionItemMapper.insert(inspectionItem);
        // 返回
        return inspectionItem.getId();
    }

    @Override
    public void updateInspectionItem(InspectionItemSaveReqVO updateReqVO) {
        // 校验存在
        validateInspectionItemExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 更新
        InspectionItemDO updateObj = BeanUtils.toBean(updateReqVO, InspectionItemDO.class);
        inspectionItemMapper.updateById(updateObj);
    }

    @Override
    public void deleteInspectionItem(Long id) {
        // 校验存在
        validateInspectionItemExists(id);
        // 删除
        inspectionItemMapper.deleteById(id);
    }

    private void validateInspectionItemExists(Long id) {
        if (inspectionItemMapper.selectById(id) == null) {
            throw exception(INSPECTION_ITEM_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        InspectionItemDO inspectionItem = inspectionItemMapper.selectByCode(code);
        if (inspectionItem == null) {
            return;
        }
        // 如果 id 为空，说明是新增，直接抛出异常
        if (id == null) {
            throw exception(INSPECTION_ITEM_CODE_DUPLICATE);
        }
        // 如果 id 不为空，且不是当前记录，则抛出异常
        if (!inspectionItem.getId().equals(id)) {
            throw exception(INSPECTION_ITEM_CODE_DUPLICATE);
        }
    }

    @Override
    public InspectionItemDO getInspectionItem(Long id) {
        return inspectionItemMapper.selectById(id);
    }

    @Override
    public List<InspectionItemDO> getInspectionItemList(Collection<Long> ids) {
        return inspectionItemMapper.selectByIds(ids);
    }

    @Override
    public PageResult<InspectionItemDO> getInspectionItemPage(InspectionItemPageReqVO pageReqVO) {
        return inspectionItemMapper.selectPage(pageReqVO);
    }

}
