package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.digest.FmsDigestSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsDigestDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 常用摘要 Service 接口
 *
 * @author 智云
 */
public interface FmsDigestService {

    Long createDigest(@Valid FmsDigestSaveReqVO createReqVO);

    void updateDigest(@Valid FmsDigestSaveReqVO updateReqVO);

    void deleteDigest(Long accountSetId, Long id);

    List<FmsDigestDO> getDigestList(Long accountSetId);

}
