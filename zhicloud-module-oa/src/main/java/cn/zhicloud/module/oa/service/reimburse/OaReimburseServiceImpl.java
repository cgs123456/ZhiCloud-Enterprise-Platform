package cn.zhicloud.module.oa.service.reimburse;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimburseItemVO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimbursePageReqVO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimburseSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseItemDO;
import cn.zhicloud.module.oa.dal.mysql.reimburse.OaReimburseItemMapper;
import cn.zhicloud.module.oa.dal.mysql.reimburse.OaReimburseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_REIMBURSE_NO_DUPLICATE;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_REIMBURSE_NOT_EXISTS;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_REIMBURSE_STATUS_INVALID;

/**
 * OA 报销单 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class OaReimburseServiceImpl implements OaReimburseService {

    /**
     * 草稿状态
     */
    private static final int STATUS_DRAFT = 10;
    /**
     * 审批中状态
     */
    private static final int STATUS_APPROVING = 20;
    /**
     * 未支付
     */
    private static final int PAYMENT_UNPAID = 10;

    /**
     * 报销审批对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "oa_reimburse";

    @Resource
    private OaReimburseMapper reimburseMapper;
    @Resource
    private OaReimburseItemMapper reimburseItemMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReimburse(OaReimburseSaveReqVO createReqVO) {
        // 校验报销单号唯一
        validateNoUnique(null, createReqVO.getNo());
        // 插入报销单（默认草稿状态、未支付）
        OaReimburseDO reimburse = BeanUtils.toBean(createReqVO, OaReimburseDO.class);
        if (reimburse.getStatus() == null) {
            reimburse.setStatus(STATUS_DRAFT);
        }
        if (reimburse.getPaymentStatus() == null) {
            reimburse.setPaymentStatus(PAYMENT_UNPAID);
        }
        reimburseMapper.insert(reimburse);
        // 插入报销明细，并回填报销总额
        saveItems(reimburse.getId(), createReqVO.getItems());
        return reimburse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReimburse(OaReimburseSaveReqVO updateReqVO) {
        // 校验存在 & 状态（仅草稿可修改）
        OaReimburseDO reimburse = validateReimburseExists(updateReqVO.getId());
        if (!Integer.valueOf(STATUS_DRAFT).equals(reimburse.getStatus())) {
            throw exception(OA_REIMBURSE_STATUS_INVALID);
        }
        // 校验报销单号唯一
        validateNoUnique(updateReqVO.getId(), updateReqVO.getNo());
        // 更新报销单（P1 修复：屏蔽 status/paymentStatus 字段，状态变更必须走 submit/cancel/payment 专门方法）
        OaReimburseDO updateObj = BeanUtils.toBean(updateReqVO, OaReimburseDO.class);
        updateObj.setStatus(null);
        updateObj.setPaymentStatus(null);
        reimburseMapper.updateById(updateObj);
        // 重建报销明细
        reimburseItemMapper.deleteByReimburseId(updateReqVO.getId());
        saveItems(updateReqVO.getId(), updateReqVO.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReimburse(Long id) {
        validateReimburseExists(id);
        reimburseMapper.deleteById(id);
        reimburseItemMapper.deleteByReimburseId(id);
    }

    @Override
    public OaReimburseDO getReimburse(Long id) {
        return reimburseMapper.selectById(id);
    }

    @Override
    public PageResult<OaReimburseDO> getReimbursePage(OaReimbursePageReqVO pageReqVO) {
        return reimburseMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReimburse(Long id) {
        // 校验存在 & 状态（仅草稿可提交）
        OaReimburseDO reimburse = validateReimburseExists(id);
        if (!Integer.valueOf(STATUS_DRAFT).equals(reimburse.getStatus())) {
            throw exception(OA_REIMBURSE_STATUS_INVALID);
        }
        // 1. 先更新状态为审批中（BPM 调用失败时本地事务自动回滚，避免孤儿状态）
        OaReimburseDO preUpdate = new OaReimburseDO();
        preUpdate.setId(id);
        preUpdate.setStatus(STATUS_APPROVING);
        reimburseMapper.updateById(preUpdate);
        // 2. 发起 BPM 流程
        Long userId = reimburse.getApplicantUserId() != null
                ? reimburse.getApplicantUserId()
                : SecurityFrameworkUtils.getLoginUserId();
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("reimburseId", id);
        if (reimburse.getTotalAmount() != null) {
            processInstanceVariables.put("totalAmount", reimburse.getTotalAmount());
        }
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(id)));
        // 3. 回填工作流编号（BPM 已成功创建，此处失败仅影响追溯，可通过 businessKey 反查）
        OaReimburseDO postUpdate = new OaReimburseDO();
        postUpdate.setId(id);
        postUpdate.setProcessInstanceId(processInstanceId);
        reimburseMapper.updateById(postUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReimburseStatus(Long id, Integer status, String processInstanceId) {
        validateReimburseExists(id);
        OaReimburseDO updateObj = new OaReimburseDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        updateObj.setProcessInstanceId(processInstanceId);
        reimburseMapper.updateById(updateObj);
    }

    private void validateNoUnique(Long id, String no) {
        if (no == null) {
            return;
        }
        OaReimburseDO reimburse = reimburseMapper.selectByNo(no);
        if (reimburse == null) {
            return;
        }
        if (id == null || !reimburse.getId().equals(id)) {
            throw exception(OA_REIMBURSE_NO_DUPLICATE);
        }
    }

    private OaReimburseDO validateReimburseExists(Long id) {
        OaReimburseDO reimburse = reimburseMapper.selectById(id);
        if (reimburse == null) {
            throw exception(OA_REIMBURSE_NOT_EXISTS);
        }
        return reimburse;
    }

    /**
     * 保存报销明细，并回填报销总额
     */
    private void saveItems(Long reimburseId, List<OaReimburseItemVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        List<OaReimburseItemDO> itemDOs = new ArrayList<>(items.size());
        for (OaReimburseItemVO item : items) {
            OaReimburseItemDO itemDO = BeanUtils.toBean(item, OaReimburseItemDO.class);
            itemDO.setId(null);
            itemDO.setReimburseId(reimburseId);
            itemDOs.add(itemDO);
            if (itemDO.getAmount() != null) {
                total = total.add(itemDO.getAmount());
            }
        }
        reimburseItemMapper.insertBatch(itemDOs);
        OaReimburseDO updateObj = new OaReimburseDO();
        updateObj.setId(reimburseId);
        updateObj.setTotalAmount(total);
        reimburseMapper.updateById(updateObj);
    }

}
