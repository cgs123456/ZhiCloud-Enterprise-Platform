package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.module.fms.controller.admin.config.vo.accountuser.FmsAccountUserRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountuser.FmsAccountUserUpdateMemberReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountuser.FmsAccountUserUpdateReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountUserDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsAccountUserMapper;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.ACCOUNT_SET_NOT_EXISTS;

/**
 * FMS 账套成员 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsAccountUserServiceImpl implements FmsAccountUserService {

    @Resource
    private FmsAccountUserMapper accountUserMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<FmsAccountUserRespVO> getAccountUserList(Long accountSetId) {
        List<FmsAccountUserDO> members = accountUserMapper.selectListByAccountSet(accountSetId);
        if (members.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量取用户与部门信息，组装昵称/部门名/手机/状态（AdminUserRespDTO 不含 email，留空）
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                members.stream().map(FmsAccountUserDO::getUserId).collect(Collectors.toSet()));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(userMap.values().stream()
                .map(AdminUserRespDTO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return members.stream().map(member -> {
            FmsAccountUserRespVO resp = new FmsAccountUserRespVO();
            resp.setUserId(member.getUserId());
            resp.setLevel(member.getLevel());
            resp.setDefaultStatus(Boolean.TRUE.equals(member.getDefaultStatus()));
            resp.setFounder(Objects.equals(String.valueOf(member.getUserId()), member.getCreator()));
            AdminUserRespDTO user = userMap.get(member.getUserId());
            if (user != null) {
                resp.setNickname(user.getNickname());
                resp.setMobile(user.getMobile());
                resp.setStatus(user.getStatus());
                DeptRespDTO dept = deptMap.get(user.getDeptId());
                resp.setDeptName(dept == null ? null : dept.getName());
            }
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public FmsAccountUserDO getAccountUser(Long accountSetId, Long userId) {
        return accountUserMapper.selectByAccountSetAndUser(accountSetId, userId);
    }

    @Override
    public void updateAccountUserList(FmsAccountUserUpdateReqVO reqVO) {
        List<FmsAccountUserDO> exists = accountUserMapper.selectListByAccountSet(reqVO.getAccountSetId());
        Map<Long, FmsAccountUserDO> existsMap = exists.stream()
                .collect(Collectors.toMap(FmsAccountUserDO::getUserId, m -> m));
        for (FmsAccountUserUpdateMemberReqVO member : reqVO.getMembers()) {
            FmsAccountUserDO existsMember = existsMap.get(member.getUserId());
            if (existsMember == null) {
                accountUserMapper.insert(FmsAccountUserDO.builder()
                        .accountSetId(reqVO.getAccountSetId()).userId(member.getUserId())
                        .level(member.getLevel()).defaultStatus(false)
                        .build());
            } else if (!Objects.equals(existsMember.getLevel(), member.getLevel())) {
                FmsAccountUserDO updateObj = new FmsAccountUserDO();
                updateObj.setId(existsMember.getId());
                updateObj.setLevel(member.getLevel());
                accountUserMapper.updateById(updateObj);
            }
        }
        // 移除本次提交中不再存在的成员（账套创建人不可移除）
        java.util.Set<Long> submitUserIds = reqVO.getMembers().stream()
                .map(FmsAccountUserUpdateMemberReqVO::getUserId).collect(Collectors.toSet());
        for (FmsAccountUserDO member : exists) {
            if (!submitUserIds.contains(member.getUserId())
                    && !Objects.equals(String.valueOf(member.getUserId()), member.getCreator())) {
                accountUserMapper.deleteById(member.getId());
            }
        }
    }

    @Override
    public void updateDefaultStatus(Long userId, Long accountSetId) {
        FmsAccountUserDO member = accountUserMapper.selectByAccountSetAndUser(accountSetId, userId);
        if (member == null) {
            throw exception(ACCOUNT_SET_NOT_EXISTS);
        }
        // 先清除该用户其它默认账套
        accountUserMapper.selectListByUser(userId).forEach(other -> {
            if (Boolean.TRUE.equals(other.getDefaultStatus()) && !Objects.equals(other.getId(), member.getId())) {
                FmsAccountUserDO updateObj = new FmsAccountUserDO();
                updateObj.setId(other.getId());
                updateObj.setDefaultStatus(false);
                accountUserMapper.updateById(updateObj);
            }
        });
        FmsAccountUserDO updateObj = new FmsAccountUserDO();
        updateObj.setId(member.getId());
        updateObj.setDefaultStatus(true);
        accountUserMapper.updateById(updateObj);
    }

}
