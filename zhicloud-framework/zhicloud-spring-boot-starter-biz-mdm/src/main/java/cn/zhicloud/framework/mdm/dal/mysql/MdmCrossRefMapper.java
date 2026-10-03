package cn.zhicloud.framework.mdm.dal.mysql;

import cn.zhicloud.framework.mdm.dal.dataobject.MdmCrossRefDO;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 主数据跨域对照 Mapper（V101）
 *
 * <p>所有查询都走 {@code (tenant_id, source_system, source_entity, source_id, target_system, target_entity)}
 * 前缀，与唯一键 {@code uk_cross_ref} 的左侧列顺序一致，因此解析与去重都能命中索引而不是全表扫描。
 *
 * <p>没有外键：映射指向的主数据在各自系统内可能被归档或清理，外键会让「清理主数据」被映射表阻断，
 * 也可能让「映射先于主数据建立」的初始化顺序无法实现。一致性由应用层查询时校验。
 *
 * @author 智云集成治理
 */
@Mapper
public interface MdmCrossRefMapper extends BaseMapperX<MdmCrossRefDO> {

    /**
     * 按完整键查询唯一映射（忽略状态），用于 upsert 的「先查后写」
     */
    default MdmCrossRefDO selectByRef(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                                      String targetSystem, String targetEntity) {
        return selectOne(new LambdaQueryWrapper<MdmCrossRefDO>()
                .eq(MdmCrossRefDO::getTenantId, tenantId)
                .eq(MdmCrossRefDO::getSourceSystem, sourceSystem)
                .eq(MdmCrossRefDO::getSourceEntity, sourceEntity)
                .eq(MdmCrossRefDO::getSourceId, sourceId)
                .eq(MdmCrossRefDO::getTargetSystem, targetSystem)
                .eq(MdmCrossRefDO::getTargetEntity, targetEntity));
    }

    /**
     * 按「源侧」查询全部目标映射：一个 ERP 产品可能对应多个系统的多个实体
     */
    default List<MdmCrossRefDO> selectListBySource(Long tenantId, String sourceSystem, String sourceEntity,
                                                   String sourceId) {
        return selectList(new LambdaQueryWrapper<MdmCrossRefDO>()
                .eq(MdmCrossRefDO::getTenantId, tenantId)
                .eq(MdmCrossRefDO::getSourceSystem, sourceSystem)
                .eq(MdmCrossRefDO::getSourceEntity, sourceEntity)
                .eq(MdmCrossRefDO::getSourceId, sourceId));
    }

}