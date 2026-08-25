package cn.zhicloud.module.hr.service.department;

import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentListReqVO;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.department.HrDepartmentDO;

import java.util.*;

/**
 * HR 部门 Service 接口
 *
 * @author zhicloud
 */
public interface HrDepartmentService {

    /**
     * 创建部门
     *
     * @param createReqVO 部门信息
     * @return 部门编号
     */
    Long createDepartment(@jakarta.validation.Valid HrDepartmentSaveReqVO createReqVO);

    /**
     * 更新部门
     *
     * @param updateReqVO 部门信息
     */
    void updateDepartment(@jakarta.validation.Valid HrDepartmentSaveReqVO updateReqVO);

    /**
     * 删除部门
     *
     * @param id 部门编号
     */
    void deleteDepartment(Long id);

    /**
     * 获得部门
     *
     * @param id 部门编号
     * @return 部门
     */
    HrDepartmentDO getDepartment(Long id);

    /**
     * 获得部门列表
     *
     * @param ids 部门编号数组
     * @return 部门列表
     */
    List<HrDepartmentDO> getDepartmentList(Collection<Long> ids);

    /**
     * 筛选部门列表
     *
     * @param reqVO 筛选条件
     * @return 部门列表
     */
    List<HrDepartmentDO> getDepartmentList(HrDepartmentListReqVO reqVO);

    /**
     * 获得指定编号的部门 Map
     *
     * @param ids 部门编号数组
     * @return 部门 Map
     */
    default Map<Long, HrDepartmentDO> getDepartmentMap(Collection<Long> ids) {
        List<HrDepartmentDO> list = getDepartmentList(ids);
        return CollectionUtils.convertMap(list, HrDepartmentDO::getId);
    }

    /**
     * 获得指定部门的所有子部门
     *
     * @param id 部门编号
     * @return 子部门列表
     */
    List<HrDepartmentDO> getChildDepartmentList(Long id);

}