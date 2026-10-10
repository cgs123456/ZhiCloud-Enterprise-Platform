package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.voucherword.FmsVoucherWordSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsVoucherWordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.VOUCHER_WORD_NOT_EXISTS;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.VOUCHER_WORD_NAME_DUPLICATE;

/**
 * FMS 凭证字 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsVoucherWordServiceImpl implements FmsVoucherWordService {

    @Resource
    private FmsVoucherWordMapper voucherWordMapper;

    @Override
    public Long createVoucherWord(FmsVoucherWordSaveReqVO createReqVO) {
        // 校验编码/名称在账套内唯一
        if (voucherWordMapper.selectByAccountSetAndUnique(createReqVO.getAccountSetId(), createReqVO.getName()) != null) {
            throw exception(VOUCHER_WORD_NAME_DUPLICATE);
        }
        FmsVoucherWordDO entity = BeanUtils.toBean(createReqVO, FmsVoucherWordDO.class);
        voucherWordMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateVoucherWord(FmsVoucherWordSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsVoucherWordDO same = voucherWordMapper.selectByAccountSetAndUnique(updateReqVO.getAccountSetId(), updateReqVO.getName());
        if (same != null && !same.getId().equals(updateReqVO.getId())) {
            throw exception(VOUCHER_WORD_NAME_DUPLICATE);
        }
        FmsVoucherWordDO updateObj = BeanUtils.toBean(updateReqVO, FmsVoucherWordDO.class);
        voucherWordMapper.updateById(updateObj);
    }

    @Override
    public void deleteVoucherWord(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        voucherWordMapper.deleteById(id);
    }

    @Override
    public List<FmsVoucherWordDO> getVoucherWordList(Long accountSetId) {
        return voucherWordMapper.selectListByAccountSet(accountSetId);
    }

    private void validateExists(Long accountSetId, Long id) {
        if (voucherWordMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(VOUCHER_WORD_NOT_EXISTS);
        }
    }

}
