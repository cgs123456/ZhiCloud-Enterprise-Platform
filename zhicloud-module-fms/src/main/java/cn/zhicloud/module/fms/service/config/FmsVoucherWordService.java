package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.voucherword.FmsVoucherWordSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsVoucherWordDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 凭证字 Service 接口
 *
 * @author 智云
 */
public interface FmsVoucherWordService {

    Long createVoucherWord(@Valid FmsVoucherWordSaveReqVO createReqVO);

    void updateVoucherWord(@Valid FmsVoucherWordSaveReqVO updateReqVO);

    void deleteVoucherWord(Long accountSetId, Long id);

    List<FmsVoucherWordDO> getVoucherWordList(Long accountSetId);

}
