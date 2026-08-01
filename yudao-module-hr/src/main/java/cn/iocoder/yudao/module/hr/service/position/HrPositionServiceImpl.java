package cn.iocoder.yudao.module.hr.service.position;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.position.vo.HrPositionPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.position.vo.HrPositionSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.position.HrPositionDO;
import cn.iocoder.yudao.module.hr.dal.mysql.position.HrPositionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.HR_POSITION_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.HR_POSITION_NOT_EXISTS;

/**
 * HR 职位 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class HrPositionServiceImpl implements HrPositionService {

    @Resource
    private HrPositionMapper positionMapper;

    @Override
    public Long createPosition(HrPositionSaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 插入
        HrPositionDO position = BeanUtils.toBean(createReqVO, HrPositionDO.class);
        positionMapper.insert(position);
        return position.getId();
    }

    @Override
    public void updatePosition(HrPositionSaveReqVO updateReqVO) {
        // 校验存在
        validatePositionExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 更新
        HrPositionDO updateObj = BeanUtils.toBean(updateReqVO, HrPositionDO.class);
        positionMapper.updateById(updateObj);
    }

    @Override
    public void deletePosition(Long id) {
        // 校验存在
        validatePositionExists(id);
        // 删除
        positionMapper.deleteById(id);
    }

    private void validatePositionExists(Long id) {
        if (positionMapper.selectById(id) == null) {
            throw exception(HR_POSITION_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        HrPositionDO position = positionMapper.selectByCode(code);
        if (position == null) {
            return;
        }
        if (id == null) {
            throw exception(HR_POSITION_CODE_DUPLICATE);
        }
        if (!position.getId().equals(id)) {
            throw exception(HR_POSITION_CODE_DUPLICATE);
        }
    }

    @Override
    public HrPositionDO getPosition(Long id) {
        return positionMapper.selectById(id);
    }

    @Override
    public List<HrPositionDO> getPositionList(Collection<Long> ids) {
        return positionMapper.selectByIds(ids);
    }

    @Override
    public PageResult<HrPositionDO> getPositionPage(HrPositionPageReqVO pageReqVO) {
        return positionMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HrPositionDO> getPositionListByDeptId(Long deptId) {
        return positionMapper.selectListByDeptId(deptId);
    }

}