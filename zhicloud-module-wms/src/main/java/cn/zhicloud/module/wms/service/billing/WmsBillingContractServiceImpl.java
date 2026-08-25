package cn.zhicloud.module.wms.service.billing;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.billing.vo.contract.WmsBillingContractItemSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.billing.vo.contract.WmsBillingContractPageReqVO;
import cn.zhicloud.module.wms.controller.admin.billing.vo.contract.WmsBillingContractSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingContractDO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingContractItemDO;
import cn.zhicloud.module.wms.dal.mysql.billing.WmsBillingContractItemMapper;
import cn.zhicloud.module.wms.dal.mysql.billing.WmsBillingContractMapper;
import cn.zhicloud.module.wms.service.md.merchant.WmsMerchantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.diffList;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.*;

/**
 * WMS 3PL 计费合同 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsBillingContractServiceImpl implements WmsBillingContractService {

    /**
     * 合同状态：10 生效
     */
    public static final int STATUS_EFFECTIVE = 10;
    /**
     * 合同状态：20 失效
     */
    public static final int STATUS_EXPIRED = 20;
    /**
     * 合同状态：30 已终止
     */
    public static final int STATUS_TERMINATED = 30;

    @Resource
    private WmsBillingContractMapper billingContractMapper;
    @Resource
    private WmsBillingContractItemMapper billingContractItemMapper;
    @Resource
    private WmsMerchantService merchantService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBillingContract(WmsBillingContractSaveReqVO createReqVO) {
        // 1. 校验计费合同保存数据
        validateBillingContractSaveData(createReqVO);

        // 2.1 插入计费合同
        WmsBillingContractDO contract = BeanUtils.toBean(createReqVO, WmsBillingContractDO.class);
        if (contract.getStatus() == null) {
            contract.setStatus(STATUS_EFFECTIVE);
        }
        billingContractMapper.insert(contract);
        // 2.2 插入计费条款
        createContractItemList(contract.getId(), createReqVO);
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBillingContract(WmsBillingContractSaveReqVO updateReqVO) {
        // 1. 校验计费合同存在
        validateBillingContractExists(updateReqVO.getId());
        // 1.1 校验计费合同保存数据
        validateBillingContractSaveData(updateReqVO);

        // 2.1 更新计费合同
        WmsBillingContractDO updateObj = BeanUtils.toBean(updateReqVO, WmsBillingContractDO.class);
        billingContractMapper.updateById(updateObj);
        // 2.2 更新计费条款
        updateContractItemList(updateReqVO.getId(), updateReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBillingContract(Long id) {
        // 1. 校验存在，且可删除
        WmsBillingContractDO contract = validateBillingContractExists(id);
        if (ObjectUtil.notEqual(contract.getStatus(), STATUS_EXPIRED)
                && ObjectUtil.notEqual(contract.getStatus(), STATUS_TERMINATED)) {
            throw exception(BILLING_CONTRACT_NOT_DELETABLE);
        }

        // 2.1 删除计费合同
        billingContractMapper.deleteById(id);
        // 2.2 删除计费条款
        billingContractItemMapper.deleteByContractId(id);
    }

    @Override
    public WmsBillingContractDO getBillingContract(Long id) {
        return billingContractMapper.selectById(id);
    }

    @Override
    public PageResult<WmsBillingContractDO> getBillingContractPage(WmsBillingContractPageReqVO pageReqVO) {
        return billingContractMapper.selectPage(pageReqVO);
    }

    @Override
    public WmsBillingContractDO validateBillingContractExists(Long id) {
        WmsBillingContractDO contract = id == null ? null : billingContractMapper.selectById(id);
        if (contract == null) {
            throw exception(BILLING_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    @Override
    public List<WmsBillingContractItemDO> getContractItemList(Long contractId) {
        return billingContractItemMapper.selectListByContractId(contractId);
    }

    private void validateBillingContractSaveData(WmsBillingContractSaveReqVO reqVO) {
        // 校验合同号唯一
        validateBillingContractNoUnique(reqVO.getId(), reqVO.getContractNo());
        // 校验货主存在
        merchantService.validateMerchantExists(reqVO.getOwnerId());
        // 校验生效/失效日期
        if (reqVO.getStartDate() != null && reqVO.getEndDate() != null
                && reqVO.getStartDate().isAfter(reqVO.getEndDate())) {
            throw exception(BILLING_CONTRACT_NOT_EXISTS);
        }
    }

    private void validateBillingContractNoUnique(Long id, String contractNo) {
        WmsBillingContractDO contract = billingContractMapper.selectByNo(contractNo);
        if (contract == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(contract.getId(), id)) {
            throw exception(BILLING_CONTRACT_NO_DUPLICATE);
        }
    }

    private void createContractItemList(Long contractId, WmsBillingContractSaveReqVO reqVO) {
        List<WmsBillingContractItemDO> list = buildContractItemList(reqVO);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(item -> item.setId(null).setContractId(contractId));
        billingContractItemMapper.insertBatch(list);
    }

    private void updateContractItemList(Long contractId, WmsBillingContractSaveReqVO reqVO) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<WmsBillingContractItemDO> oldList = billingContractItemMapper.selectListByContractId(contractId);
        List<WmsBillingContractItemDO> list = buildContractItemList(reqVO);
        List<WmsBillingContractItemDO> newList = CollUtil.isEmpty(list) ? ListUtil.of() : list;
        List<List<WmsBillingContractItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> ObjectUtil.equal(oldVal.getId(), newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(item -> item.setContractId(contractId));
            billingContractItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            diffList.get(1).forEach(item -> item.setContractId(contractId));
            billingContractItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            billingContractItemMapper.deleteByIds(convertList(diffList.get(2), WmsBillingContractItemDO::getId));
        }
    }

    private List<WmsBillingContractItemDO> buildContractItemList(WmsBillingContractSaveReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getItems())) {
            return ListUtil.of();
        }
        return convertList(reqVO.getItems(), item -> BeanUtils.toBean(item, WmsBillingContractItemDO.class));
    }

}
