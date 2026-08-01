package cn.iocoder.yudao.module.qms.service.sqm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.ScarPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.ScarSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.ScarDO;
import cn.iocoder.yudao.module.qms.dal.mysql.sqm.ScarMapper;
import cn.iocoder.yudao.module.qms.enums.qms.ScarStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.SCAR_NOT_EXISTS;

/**
 * QMS SCAR（供应商纠正措施请求）Service 实现类
 *
 * @author yudao
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
        if (scar.getStatus() == null) {
            scar.setStatus(ScarStatusEnum.OPEN.getStatus());
        }
        scarMapper.insert(scar);
        return scar.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScar(ScarSaveReqVO updateReqVO) {
        validateScarExists(updateReqVO.getId());
        ScarDO updateObj = BeanUtils.toBean(updateReqVO, ScarDO.class);
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