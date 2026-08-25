package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount.ErpGlAccountSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 会计科目 Service 接口（P0-7）
 *
 * <p>提供会计科目 CRUD + 树形结构查询 + 余额查询。
 *
 * @author 智云
 */
public interface ErpGlAccountService {

    /**
     * 创建会计科目
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createGlAccount(@Valid ErpGlAccountSaveReqVO createReqVO);

    /**
     * 更新会计科目
     *
     * @param updateReqVO 更新信息
     */
    void updateGlAccount(@Valid ErpGlAccountSaveReqVO updateReqVO);

    /**
     * 删除会计科目
     *
     * @param id 编号
     */
    void deleteGlAccount(Long id);

    /**
     * 获取会计科目
     *
     * @param id 编号
     * @return 科目
     */
    ErpGlAccountDO getGlAccount(Long id);

    /**
     * 根据编码获取会计科目
     *
     * @param code 科目编码
     * @return 科目
     */
    ErpGlAccountDO getGlAccountByCode(String code);

    /**
     * 分页查询会计科目
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ErpGlAccountDO> getGlAccountPage(ErpGlAccountPageReqVO pageReqVO);

    /**
     * 获取所有会计科目列表（用于树形构建）
     *
     * @return 科目列表
     */
    List<ErpGlAccountDO> getGlAccountList();

    /**
     * 获取末级科目列表（用于凭证分录选择）
     *
     * @return 末级科目列表
     */
    List<ErpGlAccountDO> getLeafGlAccountList();

    /**
     * 获取子科目列表
     *
     * @param parentId 父级编号
     * @return 子科目列表
     */
    List<ErpGlAccountDO> getGlAccountListByParentId(Long parentId);

    /**
     * 校验科目存在并启用
     *
     * @param id 编号
     * @return 科目
     */
    ErpGlAccountDO validateGlAccountExists(Long id);

    /**
     * 校验科目是末级科目（用于凭证录入）
     *
     * @param id 编号
     * @return 科目
     */
    ErpGlAccountDO validateGlAccountIsLeaf(Long id);

}
