package cn.zhicloud.module.oa.service.knowledge;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionPageReqVO;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeVersionDO;
import cn.zhicloud.module.oa.dal.mysql.knowledge.OaKnowledgeVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_KNOWLEDGE_VERSION_NOT_EXISTS;

/**
 * OA 知识库版本 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class OaKnowledgeVersionServiceImpl implements OaKnowledgeVersionService {

    @Resource
    private OaKnowledgeVersionMapper versionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVersion(OaKnowledgeVersionSaveReqVO createReqVO) {
        OaKnowledgeVersionDO version = BeanUtils.toBean(createReqVO, OaKnowledgeVersionDO.class);
        versionMapper.insert(version);
        return version.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVersion(OaKnowledgeVersionSaveReqVO updateReqVO) {
        validateVersionExists(updateReqVO.getId());
        OaKnowledgeVersionDO updateObj = BeanUtils.toBean(updateReqVO, OaKnowledgeVersionDO.class);
        versionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(Long id) {
        validateVersionExists(id);
        versionMapper.deleteById(id);
    }

    @Override
    public OaKnowledgeVersionDO getVersion(Long id) {
        return versionMapper.selectById(id);
    }

    @Override
    public PageResult<OaKnowledgeVersionDO> getVersionPage(OaKnowledgeVersionPageReqVO pageReqVO) {
        return versionMapper.selectPage(pageReqVO);
    }

    @Override
    public List<OaKnowledgeVersionDO> getVersionListByArticleId(Long articleId) {
        return versionMapper.selectListByArticleId(articleId);
    }

    private OaKnowledgeVersionDO validateVersionExists(Long id) {
        OaKnowledgeVersionDO version = versionMapper.selectById(id);
        if (version == null) {
            throw exception(OA_KNOWLEDGE_VERSION_NOT_EXISTS);
        }
        return version;
    }

}
