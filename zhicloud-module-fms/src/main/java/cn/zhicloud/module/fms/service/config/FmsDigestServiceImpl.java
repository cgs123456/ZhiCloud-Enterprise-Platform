package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.digest.FmsDigestSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsDigestDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsDigestMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.DIGEST_NOT_EXISTS;

/**
 * FMS 常用摘要 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsDigestServiceImpl implements FmsDigestService {

    @Resource
    private FmsDigestMapper digestMapper;

    @Override
    public Long createDigest(FmsDigestSaveReqVO createReqVO) {
        FmsDigestDO entity = BeanUtils.toBean(createReqVO, FmsDigestDO.class);
        digestMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateDigest(FmsDigestSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsDigestDO updateObj = BeanUtils.toBean(updateReqVO, FmsDigestDO.class);
        digestMapper.updateById(updateObj);
    }

    @Override
    public void deleteDigest(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        digestMapper.deleteById(id);
    }

    @Override
    public List<FmsDigestDO> getDigestList(Long accountSetId) {
        return digestMapper.selectListByAccountSet(accountSetId);
    }

    private void validateExists(Long accountSetId, Long id) {
        if (digestMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(DIGEST_NOT_EXISTS);
        }
    }

}
