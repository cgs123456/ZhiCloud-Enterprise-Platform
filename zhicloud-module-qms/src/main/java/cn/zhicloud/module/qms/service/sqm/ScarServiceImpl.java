package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.ScarPageReqVO;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.ScarSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.ScarDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.ScarMapper;
import cn.zhicloud.module.qms.enums.qms.ScarStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.SCAR_NOT_EXISTS;

/**
 * QMS SCAR（供应商纠正措施请求）Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class ScarServiceImpl implements ScarService {

    @Resource
    private ScarMapper scarMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScar(ScarSaveReqVO createReqVO) {
        ScarDO scar = BeanUtils.toBean(createReqVO, ScarDO.class);
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        scar.setStatus(ScarStatusEnum.OPEN.getStatus());
        scarMapper.insert(scar);
        return scar.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScar(ScarSaveReqVO updateReqVO) {
        validateScarExists(updateReqVO.getId());
        ScarDO updateObj = BeanUtils.toBean(updateReqVO, ScarDO.class);
        // 状态不接受外部写入：状态变更必须走 closeScar（§12.236.7）。
        // MyBatis-Plus 的 updateById 跳过 null 字段，故置 null 的效果是
        // 该列不进 UPDATE 语句 —— 调用方的 status 无法生效。
        updateObj.setStatus(null);
        scarMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScar(Long id) {
        validateScarExists(id);
        scarMapper.deleteById(id);
    }

    private void validateScarExists(Long id) {
        if (scarMapper.selectById(id) == null) {
            throw exception(SCAR_NOT_EXISTS);
        }
    }

    @Override
    public ScarDO getScar(Long id) {
        return scarMapper.selectById(id);
    }

    @Override
    public PageResult<ScarDO> getScarPage(ScarPageReqVO pageReqVO) {
        return scarMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeScar(Long id) {
        ScarDO scar = scarMapper.selectById(id);
        if (scar == null) {
            throw exception(SCAR_NOT_EXISTS);
        }
        ScarDO updateObj = new ScarDO();
        updateObj.setId(id);
        updateObj.setStatus(ScarStatusEnum.CLOSED.getStatus());
        updateObj.setCloseTime(LocalDateTime.now());
        scarMapper.updateById(updateObj);
    }

}