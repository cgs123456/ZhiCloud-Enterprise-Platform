package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 薪资配置 Mapper（单条）
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryConfigMapper extends BaseMapperX<HrmSalaryConfigDO> {

    /**
     * 取唯一配置行（无则 null）
     */
    default HrmSalaryConfigDO selectSingle() {
        List<HrmSalaryConfigDO> list = selectList(new LambdaQueryWrapperX<HrmSalaryConfigDO>()
                .orderByAsc(HrmSalaryConfigDO::getId));
        return list.isEmpty() ? null : list.get(0);
    }

}
