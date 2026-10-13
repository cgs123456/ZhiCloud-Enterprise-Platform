package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.api.ErpGlVoucherApi;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link ErpGlVoucherApi} 的 ERP 侧实现
 *
 * <p>V193（台账 §12.213）：FMS 账套详情页查「本账套凭证流水」的跨模块只读入口。
 * 账套→账簿的解析在 {@link ErpGlVoucherServiceImpl#getGlVoucherPage} 内完成。
 *
 * @author 智云
 */
@Component
public class ErpGlVoucherApiImpl implements ErpGlVoucherApi {

    @Resource
    private ErpGlVoucherService glVoucherService;

    @Override
    public PageResult<ErpGlVoucherRespVO> getGlVoucherPageByAccountSetId(ErpGlVoucherPageReqVO pageReqVO) {
        if (pageReqVO == null || pageReqVO.getAccountSetId() == null) {
            return new PageResult<>(List.of(), 0L);
        }
        PageResult<ErpGlVoucherDO> page = glVoucherService.getGlVoucherPage(pageReqVO);
        if (page == null || page.getList() == null || page.getList().isEmpty()) {
            return new PageResult<>(List.of(), page == null ? 0L : page.getTotal());
        }
        List<ErpGlVoucherRespVO> respList = ErpGlVoucherConvert.convertList(page.getList());
        return new PageResult<>(respList, page.getTotal());
    }

}
