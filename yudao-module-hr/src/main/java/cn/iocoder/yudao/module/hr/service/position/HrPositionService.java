package cn.iocoder.yudao.module.hr.service.position;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.position.vo.HrPositionPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.position.vo.HrPositionSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.position.HrPositionDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * HR 职位 Service 接口
 *
 * @author yudao
 */
public interface HrPositionService {

    /**
     * 创建职位
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPosition(@Valid HrPositionSaveReqVO createReqVO);

    /**
     * 更新职位
     *
     * @param updateReqVO 更新信息
     */
    void updatePosition(@Valid HrPositionSaveReqVO updateReqVO);

    /**
     * 删除职位
     *
     * @param id 编号
     */
    void deletePosition(Long id);

    /**
     * 获得职位
     *
     * @param id 编号
     * @return 职位
     */
    HrPositionDO getPosition(Long id);

    /**
     * 获得职位列表
     *
     * @param ids 编号数组
     * @return 职位列表
     */
    List<HrPositionDO> getPositionList(Collection<Long> ids);

    /**
     * 获得职位分页
     *
     * @param pageReqVO 分页查询
     * @return 职位分页
     */
    PageResult<HrPositionDO> getPositionPage(HrPositionPageReqVO pageReqVO);

    /**
     * 获得指定部门的职位列表
     *
     * @param deptId 部门编号
     * @return 职位列表
     */
    List<HrPositionDO> getPositionListByDeptId(Long deptId);

}