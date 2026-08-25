package cn.zhicloud.module.system.dal.mysql.logger;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import cn.zhicloud.module.system.dal.dataobject.logger.OperateLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperateLogMapper extends BaseMapperX<OperateLogDO> {

    default PageResult<OperateLogDO> selectPage(OperateLogPageReqVO pageReqDTO) {
        return selectPage(pageReqDTO, new LambdaQueryWrapperX<OperateLogDO>()
                .eqIfPresent(OperateLogDO::getUserId, pageReqDTO.getUserId())
                .eqIfPresent(OperateLogDO::getBizId, pageReqDTO.getBizId())
                .likeIfPresent(OperateLogDO::getType, pageReqDTO.getType())
                .likeIfPresent(OperateLogDO::getSubType, pageReqDTO.getSubType())
                .likeIfPresent(OperateLogDO::getAction, pageReqDTO.getAction())
                .betweenIfPresent(OperateLogDO::getCreateTime, pageReqDTO.getCreateTime())
                .orderByDesc(OperateLogDO::getId));
    }

    default PageResult<OperateLogDO> selectPage(OperateLogPageReqDTO pageReqDTO) {
        return selectPage(pageReqDTO, new LambdaQueryWrapperX<OperateLogDO>()
                .eqIfPresent(OperateLogDO::getType, pageReqDTO.getType())
                .eqIfPresent(OperateLogDO::getBizId, pageReqDTO.getBizId())
                .eqIfPresent(OperateLogDO::getUserId, pageReqDTO.getUserId())
                .orderByDesc(OperateLogDO::getId));
    }

    /**
     * 查询当前租户下最新一条日志的 current_hash
     *
     * 用于 Hash 链式审计：插入新日志前获取上一条日志的 hash 作为 prev_hash
     * 注意：多租户场景下，MyBatis Plus 的租户拦截器会自动追加 tenant_id 条件
     *
     * @return 最新一条日志的 current_hash；若表中无记录返回 null
     */
    @Select("SELECT current_hash FROM system_operate_log WHERE deleted = 0 ORDER BY id DESC LIMIT 1")
    String selectLatestCurrentHash();

    /**
     * 根据 ID 范围（>= startId）按 ID 升序查询日志，用于 Hash 链完整性验证
     *
     * 注意：多租户场景下，MyBatis Plus 的租户拦截器会自动追加 tenant_id 条件
     *
     * @param startId 起始 ID（包含）
     * @param limit   最大返回条数（避免一次性加载过多）
     * @return 日志列表
     */
    @Select("SELECT * FROM system_operate_log WHERE deleted = 0 AND id >= #{startId} ORDER BY id ASC LIMIT #{limit}")
    List<OperateLogDO> selectListForHashVerify(@Param("startId") Long startId, @Param("limit") Integer limit);

}
