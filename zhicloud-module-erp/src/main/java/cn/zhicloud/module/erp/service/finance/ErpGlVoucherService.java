package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 会计凭证 Service 接口（P0-7）
 *
 * <p>提供会计凭证 CRUD + 审核/反审核 + 余额维护。
 *
 * <p>核心规则：
 * <ul>
 *   <li>凭证分录必须满足"有借必有贷，借贷必相等"</li>
 *   <li>审核后更新对应科目的累计发生额和期末余额</li>
 *   <li>反审核回滚科目余额，凭证回到草稿状态</li>
 * </ul>
 *
 * @author 智云
 */
public interface ErpGlVoucherService {

    /**
     * 创建会计凭证
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createGlVoucher(@Valid ErpGlVoucherSaveReqVO createReqVO);

    /**
     * 更新会计凭证（仅草稿状态可更新）
     *
     * @param updateReqVO 更新信息
     */
    void updateGlVoucher(@Valid ErpGlVoucherSaveReqVO updateReqVO);

    /**
     * 删除会计凭证（仅草稿状态可删除）
     *
     * @param id 编号
     */
    void deleteGlVoucher(Long id);

    /**
     * 获取会计凭证（含分录）
     *
     * @param id 编号
     * @return 凭证
     */
    ErpGlVoucherDO getGlVoucher(Long id);

    /**
     * 获取凭证分录列表
     *
     * @param voucherId 凭证编号
     * @return 分录列表
     */
    List<ErpGlVoucherEntryDO> getGlVoucherEntryList(Long voucherId);

    /**
     * 分页查询会计凭证
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ErpGlVoucherDO> getGlVoucherPage(ErpGlVoucherPageReqVO pageReqVO);

    /**
     * 审核会计凭证
     *
     * <p>审核后凭证状态变为 APPROVED，且更新对应科目的累计发生额和期末余额。
     *
     * @param id 编号
     */
    void approveGlVoucher(Long id);

    /**
     * 反审核会计凭证
     *
     * <p>反审核后凭证状态变为 DRAFT，且回滚对应科目的累计发生额和期末余额。
     *
     * @param id 编号
     */
    void reverseApproveGlVoucher(Long id);

}
