package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fixedasset.ErpFixedAssetSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDepreciationDO;
import jakarta.validation.Valid;

import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 固定资产 Service 接口（P0-14）
 *
 * <p>提供固定资产主数据 CRUD + 月度折旧计提功能。
 *
 * <p>折旧公式（直线法）：
 * <pre>
 *   月折旧额 = (原值 - 残值) / 使用年限月数
 *   累计折旧 = 月折旧额 × 已折旧月数
 *   账面净值 = 原值 - 累计折旧
 *   提足判断：已折旧月数 >= 使用年限月数，或 账面净值 <= 残值
 * </pre>
 *
 * @author 芋道源码
 */
public interface ErpFixedAssetService {

    // ==================== 固定资产主数据 CRUD ====================

    /**
     * 创建固定资产
     */
    Long createFixedAsset(@Valid ErpFixedAssetSaveReqVO createReqVO);

    /**
     * 更新固定资产
     */
    void updateFixedAsset(@Valid ErpFixedAssetSaveReqVO updateReqVO);

    /**
     * 删除固定资产（无折旧记录时可删）
     */
    void deleteFixedAsset(Long id);

    /**
     * 获取固定资产
     */
    ErpFixedAssetDO getFixedAsset(Long id);

    /**
     * 分页查询固定资产
     */
    PageResult<ErpFixedAssetDO> getFixedAssetPage(ErpFixedAssetPageReqVO pageReqVO);

    // ==================== 折旧计提 ====================

    /**
     * 计提月度折旧（核心方法）
     *
     * <p>逻辑：
     * <ol>
     *   <li>校验资产存在 + 状态为在用</li>
     *   <li>校验资产未提足折旧（已折旧月数 < 使用年限月数 且 账面净值 > 残值）</li>
     *   <li>校验该期间无重复折旧记录</li>
     *   <li>按直线法计算月折旧额</li>
     *   <li>创建折旧记录（状态=待审核）</li>
     *   <li>更新资产累计折旧/已折旧月数/账面净值</li>
     * </ol>
     *
     * @param fixedAssetId 固定资产编号
     * @param periodId     会计期间编号
     * @return 折旧记录编号
     */
    Long calculateMonthlyDepreciation(Long fixedAssetId, Long periodId);

    /**
     * 审核折旧记录（待审核 → 已审核）
     *
     * <p>审核后折旧记录不可修改。后续可扩展生成 GL 凭证。
     *
     * @param depreciationId 折旧记录编号
     */
    void approveDepreciation(Long depreciationId);

    /**
     * 查询某资产的折旧记录列表
     */
    List<ErpFixedAssetDepreciationDO> getDepreciationListByFixedAssetId(Long fixedAssetId);

    /**
     * 查询某期间下的折旧记录列表
     */
    List<ErpFixedAssetDepreciationDO> getDepreciationListByPeriodId(Long periodId);

    /**
     * 校验固定资产是否存在
     */
    ErpFixedAssetDO validateFixedAssetExists(Long id);

    /**
     * 批量计提月度折旧
     *
     * <p>按资产 ID 列表批量计提月度折旧，同一期间不能重复计提。
     * 单个资产计提失败不影响其他资产。
     *
     * @param assetIds 资产 ID 列表
     * @param period   计提期间（按月份定位会计期间）
     * @return 成功计提的资产数和总金额
     */
    BatchDepreciationResult batchCalculateMonthlyDepreciation(List<Long> assetIds, LocalDate period);

    /**
     * 批量折旧计提结果
     */
    class BatchDepreciationResult {
        /** 成功计提的资产数 */
        private final int successCount;
        /** 成功计提的总金额 */
        private final BigDecimal totalAmount;

        public BatchDepreciationResult(int successCount, BigDecimal totalAmount) {
            this.successCount = successCount;
            this.totalAmount = totalAmount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

}
