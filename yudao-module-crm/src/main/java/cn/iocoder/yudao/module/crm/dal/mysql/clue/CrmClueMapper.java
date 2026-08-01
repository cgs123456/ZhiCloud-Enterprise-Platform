package cn.iocoder.yudao.module.crm.dal.mysql.clue;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.CrmCluePageReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.clue.CrmClueDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import cn.iocoder.yudao.module.crm.enums.common.CrmBizTypeEnum;
import cn.iocoder.yudao.module.crm.enums.common.CrmSceneTypeEnum;
import cn.iocoder.yudao.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索 Mapper
 *
 * @author Wanwan
 */
@Mapper
public interface CrmClueMapper extends BaseMapperX<CrmClueDO> {

    default int updateOwnerUserIdById(Long id, Long ownerUserId) {
        return update(new LambdaUpdateWrapper<CrmClueDO>()
                .eq(CrmClueDO::getId, id)
                .set(CrmClueDO::getOwnerUserId, ownerUserId));
    }

    default List<CrmClueDO> selectListByAutoPool(CrmCluePoolConfigDO poolConfig) {
        LambdaQueryWrapper<CrmClueDO> query = new LambdaQueryWrapper<>();
        query.gt(CrmClueDO::getOwnerUserId, 0);
        // 未转化
        query.eq(CrmClueDO::getTransformStatus, false);
        // 已经超时未跟进
        LocalDateTime contactExpireTime = LocalDateTime.now().minusDays(poolConfig.getContactExpireDays());
        query.and(q -> q.lt(CrmClueDO::getContactLastTime, contactExpireTime)
                .or().isNull(CrmClueDO::getContactLastTime));
        return selectList(query);
    }

    default PageResult<CrmClueDO> selectPage(CrmCluePageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmClueDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CLUE.getType(),
                CrmClueDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmClueDO.class)
                .likeIfPresent(CrmClueDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmClueDO::getTransformStatus, pageReqVO.getTransformStatus())
                .likeIfPresent(CrmClueDO::getTelephone, pageReqVO.getTelephone())
                .likeIfPresent(CrmClueDO::getMobile, pageReqVO.getMobile())
                .eqIfPresent(CrmClueDO::getIndustryId, pageReqVO.getIndustryId())
                .eqIfPresent(CrmClueDO::getLevel, pageReqVO.getLevel())
                .eqIfPresent(CrmClueDO::getSource, pageReqVO.getSource())
                .eqIfPresent(CrmClueDO::getFollowUpStatus, pageReqVO.getFollowUpStatus())
                .betweenIfPresent(CrmClueDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(CrmClueDO::getId);
        return selectJoinPage(pageReqVO, CrmClueDO.class, query);
    }

    default Long selectCountByFollow(Long userId) {
        MPJLambdaWrapperX<CrmClueDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CLUE.getType(),
                CrmClueDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 未跟进 + 未转化
        query.eq(CrmClueDO::getFollowUpStatus, false)
                .eq(CrmClueDO::getTransformStatus, false);
        return selectCount(query);
    }

}
