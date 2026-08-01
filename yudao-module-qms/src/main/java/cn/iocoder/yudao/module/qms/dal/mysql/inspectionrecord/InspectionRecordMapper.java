package cn.iocoder.yudao.module.qms.dal.mysql.inspectionrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * QMS 检验记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface InspectionRecordMapper extends BaseMapperX<InspectionRecordDO> {

    default PageResult<InspectionRecordDO> selectPage(InspectionRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<InspectionRecordDO>()
                .eqIfPresent(InspectionRecordDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(InspectionRecordDO::getItemId, reqVO.getItemId())
                .eqIfPresent(InspectionRecordDO::getResult, reqVO.getResult())
                .eqIfPresent(InspectionRecordDO::getInspector, reqVO.getInspector())
                .betweenIfPresent(InspectionRecordDO::getInspectTime, reqVO.getInspectTime())
                .orderByDesc(InspectionRecordDO::getId));
    }

    default List<InspectionRecordDO> selectListByOrderId(Long orderId) {
        return selectList(InspectionRecordDO::getOrderId, orderId);
    }

    /**
     * 按检验项目 ID 查询检验记录列表
     *
     * @param itemId 检验项目 ID
     * @return 检验记录列表（按创建时间倒序）
     */
    @Select("SELECT * FROM qms_inspection_record WHERE item_id = #{itemId} AND deleted = 0 ORDER BY create_time DESC")
    List<InspectionRecordDO> selectListByItemId(Long itemId);

}
