package io.choerodon.devops.app.service.impl;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.choerodon.base.domain.PageRequest;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.app.service.DevopsMergeRequestService;
import io.choerodon.devops.infra.dto.DevopsMergeRequestDTO;
import io.choerodon.devops.infra.mapper.DevopsMergeRequestMapper;
import io.choerodon.devops.infra.util.PageRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Sheep on 2019/7/15.
 */
@Service
public class DevopsMergeRequestServiceImpl implements DevopsMergeRequestService {


    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsMergeRequestServiceImpl.class);


    @Autowired
    private DevopsMergeRequestMapper devopsMergeRequestMapper;


    @Override
    public List<DevopsMergeRequestDTO> baseListBySourceBranch(String sourceBranchName, Long gitLabProjectId) {
        return devopsMergeRequestMapper.listBySourceBranch(gitLabProjectId.intValue(),sourceBranchName);
    }

    @Override
    public DevopsMergeRequestDTO baseQueryByAppIdAndMergeRequestId(Long projectId, Long gitlabMergeRequestId) {
        DevopsMergeRequestDTO devopsMergeRequestDTO = new DevopsMergeRequestDTO();
        devopsMergeRequestDTO.setProjectId(projectId);
        devopsMergeRequestDTO.setGitlabMergeRequestId(gitlabMergeRequestId);
        return devopsMergeRequestMapper
                .selectOne(devopsMergeRequestDTO);
    }

    @Override
    public PageInfo<DevopsMergeRequestDTO> basePageByOptions(Integer gitlabProjectId, String state, PageRequest pageRequest) {
        DevopsMergeRequestDTO devopsMergeRequestDTO = new DevopsMergeRequestDTO();
        devopsMergeRequestDTO.setProjectId(gitlabProjectId.longValue());
        devopsMergeRequestDTO.setState(state);
        PageInfo<DevopsMergeRequestDTO> devopsMergeRequestDTOPageInfo= PageHelper.startPage(pageRequest.getPage(),pageRequest.getSize(), PageRequestUtil.getOrderBy(pageRequest)).doSelectPageInfo(() ->
                devopsMergeRequestMapper.select(devopsMergeRequestDTO));
        return devopsMergeRequestDTOPageInfo;
    }

    @Override
    public List<DevopsMergeRequestDTO> baseQueryByGitlabProjectId(Integer gitlabProjectId) {
        DevopsMergeRequestDTO devopsMergeRequestDTO = new DevopsMergeRequestDTO();
        devopsMergeRequestDTO.setProjectId(gitlabProjectId.longValue());
        List<DevopsMergeRequestDTO> devopsMergeRequestDTOS = devopsMergeRequestMapper
                .select(devopsMergeRequestDTO);
        return devopsMergeRequestDTOS;
    }

    @Override
    public Integer baseUpdate(DevopsMergeRequestDTO devopsMergeRequestDTO) {
        return devopsMergeRequestMapper.updateByPrimaryKey(devopsMergeRequestDTO);
    }

    @Override
    public void baseCreate(DevopsMergeRequestDTO devopsMergeRequestDTO) {
        Long projectId = devopsMergeRequestDTO.getProjectId();
        Long gitlabMergeRequestId = devopsMergeRequestDTO.getGitlabMergeRequestId();
        DevopsMergeRequestDTO mergeRequestETemp = baseQueryByAppIdAndMergeRequestId(projectId, gitlabMergeRequestId);
        Long mergeRequestId = mergeRequestETemp != null ? mergeRequestETemp.getId() : null;
        if (mergeRequestId == null) {
            try {
                devopsMergeRequestMapper.insert(devopsMergeRequestDTO);
            } catch (Exception e) {
                LOGGER.info("error.save.merge.request");
            }
        } else {
            devopsMergeRequestDTO.setId(mergeRequestId);
            devopsMergeRequestDTO.setObjectVersionNumber(mergeRequestETemp.getObjectVersionNumber());
            if (baseUpdate(devopsMergeRequestDTO) == 0) {
                throw new CommonException("error.update.merge.request");
            }
        }
    }

    @Override
    public DevopsMergeRequestDTO baseCountMergeRequest(Integer gitlabProjectId) {
        return devopsMergeRequestMapper.countMergeRequest(gitlabProjectId);
    }


}