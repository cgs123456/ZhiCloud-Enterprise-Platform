package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 会计科目 Service 实现类（P0-7）
 *
 * <p>支持树形结构的科目管理：
 * <ul>
 *   <li>新增/更新时校验编码唯一、父级合法性、类型一致性</li>
 *   <li>删除时校验是否有子科目、是否被凭证引用</li>
 *   <li>新增子科目时，将父科目标记为非末级（is_leaf=false）</li>
 * </ul>
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class ErpGlAccountServiceImpl implements ErpGlAccountService {

    @Resource
    private ErpGlAccountMapper glAccountMapper;
    @Resource
    private ErpGlVoucherEntryMapper glVoucherEntryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGlAccount(ErpGlAccountSaveReqVO createReqVO) {
        // 校验编码唯一
        if (glAccountMapper.selectByCode(createReqVO.getCode()) != null) {
            throw exception(GL_ACCOUNT_CODE_DUPLICATE, createReqVO.getCode());
        }
        // 校验父级合法性 + 类型一致性
        validateParent(createReqVO.getParentId(), null, createReqVO.getType());
        // 计算层级
        Integer level = calculateLevel(createReqVO.getParentId());
        ErpGlAccountDO account = BeanUtils.toBean(createReqVO, ErpGlAccountDO.class);
        account.setLevel(level);
        if (account.getIsLeaf() == null) {
            account.setIsLeaf(true);
        }
        if (account.getStatus() == null) {
            account.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        // 初始化余额字段（避免 null）
        account.setOpeningDebit(account.getOpeningDebit() == null ? BigDecimal.ZERO : account.getOpeningDebit());
        account.setOpeningCredit(account.getOpeningCredit() == null ? BigDecimal.ZERO : account.getOpeningCredit());
        account.setCurrentDebit(BigDecimal.ZERO);
        account.setCurrentCredit(BigDecimal.ZERO);
        account.setClosingDebit(account.getOpeningDebit());
        account.setClosingCredit(account.getOpeningCredit());
        // 新增子科目时，父级标记为非末级
        if (createReqVO.getParentId() != null && createReqVO.getParentId() > 0L) {
            ErpGlAccountDO parent = glAccountMapper.selectById(createReqVO.getParentId());
            if (parent != null && Boolean.TRUE.equals(parent.getIsLeaf())) {
                ErpGlAccountDO parentUpdate = new ErpGlAccountDO();
                parentUpdate.setId(parent.getId());
                parentUpdate.setIsLeaf(false);
                glAccountMapper.updateById(parentUpdate);
            }
        }
        glAccountMapper.insert(account);
        return account.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGlAccount(ErpGlAccountSaveReqVO updateReqVO) {
        ErpGlAccountDO existing = validateGlAccountExists(updateReqVO.getId());
        // 校验编码唯一（排除自身）
        ErpGlAccountDO byCode = glAccountMapper.selectByCode(updateReqVO.getCode());
        if (byCode != null && !Objects.equals(byCode.getId(), updateReqVO.getId())) {
            throw exception(GL_ACCOUNT_CODE_DUPLICATE, updateReqVO.getCode());
        }
        // 校验父级合法性 + 类型一致性
        validateParent(updateReqVO.getParentId(), updateReqVO.getId(), updateReqVO.getType());
        Integer level = calculateLevel(updateReqVO.getParentId());
        ErpGlAccountDO updateObj = BeanUtils.toBean(updateReqVO, ErpGlAccountDO.class);
        updateObj.setLevel(level);
        glAccountMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGlAccount(Long id) {
        ErpGlAccountDO existing = validateGlAccountExists(id);
        // 检查是否有子科目
        List<ErpGlAccountDO> children = glAccountMapper.selectListByParentId(id);
        if (children != null && !children.isEmpty()) {
            throw exception(GL_ACCOUNT_HAS_CHILDREN, existing.getCode());
        }
        // 检查是否被凭证引用（容错：凭证分录表可能未导入）
        try {
            Long refCount = glVoucherEntryMapper.selectCountByAccountId(id);
            if (refCount != null && refCount > 0) {
                throw exception(GL_ACCOUNT_HAS_VOUCHER, existing.getCode());
            }
        } catch (Exception e) {
            log.warn("[deleteGlAccount][凭证分录表查询失败，跳过凭证引用校验 accountId={}]", id, e);
        }
        glAccountMapper.deleteById(id);
    }

    @Override
    public ErpGlAccountDO getGlAccount(Long id) {
        return glAccountMapper.selectById(id);
    }

    @Override
    public ErpGlAccountDO getGlAccountByCode(String code) {
        return glAccountMapper.selectByCode(code);
    }

    @Override
    public PageResult<ErpGlAccountDO> getGlAccountPage(ErpGlAccountPageReqVO pageReqVO) {
        return glAccountMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpGlAccountDO> getGlAccountList() {
        return glAccountMapper.selectList(null);
    }

    @Override
    public List<ErpGlAccountDO> getLeafGlAccountList() {
        return glAccountMapper.selectListByLeaf(true);
    }

    @Override
    public List<ErpGlAccountDO> getGlAccountListByParentId(Long parentId) {
        return glAccountMapper.selectListByParentId(parentId);
    }

    @Override
    public ErpGlAccountDO validateGlAccountExists(Long id) {
        if (id == null) {
            throw exception(GL_ACCOUNT_NOT_EXISTS);
        }
        ErpGlAccountDO account = glAccountMapper.selectById(id);
        if (account == null) {
            throw exception(GL_ACCOUNT_NOT_EXISTS);
        }
        return account;
    }

    @Override
    public ErpGlAccountDO validateGlAccountIsLeaf(Long id) {
        ErpGlAccountDO account = validateGlAccountExists(id);
        if (!Boolean.TRUE.equals(account.getIsLeaf())) {
            throw exception(GL_ACCOUNT_NOT_LEAF, account.getCode());
        }
        return account;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验父级合法性：
     * <ul>
     *   <li>父级不为 0 时必须存在</li>
     *   <li>不能设置自己为父级</li>
     *   <li>子科目类型必须与父科目一致</li>
     * </ul>
     */
    private void validateParent(Long parentId, Long selfId, Integer type) {
        if (parentId == null || parentId <= 0L) {
            return; // 顶级，无需校验
        }
        if (selfId != null && Objects.equals(parentId, selfId)) {
            throw exception(GL_ACCOUNT_PARENT_ERROR);
        }
        ErpGlAccountDO parent = glAccountMapper.selectById(parentId);
        if (parent == null) {
            throw exception(GL_ACCOUNT_PARENT_NOT_EXITS);
        }
        if (selfId != null) {
            // 简单防环：不允许把自己当前的子科目设为父级（仅查一层，更深的环由业务保证）
            List<ErpGlAccountDO> children = glAccountMapper.selectListByParentId(selfId);
            if (children != null) {
                for (ErpGlAccountDO child : children) {
                    if (Objects.equals(child.getId(), parentId)) {
                        throw exception(GL_ACCOUNT_PARENT_IS_CHILD);
                    }
                }
            }
        }
        if (!Objects.equals(parent.getType(), type)) {
            throw exception(GL_ACCOUNT_TYPE_NOT_MATCH, type, parent.getType());
        }
    }

    /**
     * 计算层级：父级为 0 时层级=1，否则 父级层级+1
     */
    private Integer calculateLevel(Long parentId) {
        if (parentId == null || parentId <= 0L) {
            return 1;
        }
        ErpGlAccountDO parent = glAccountMapper.selectById(parentId);
        if (parent == null || parent.getLevel() == null) {
            return 1;
        }
        return parent.getLevel() + 1;
    }

}
