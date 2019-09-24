package io.choerodon.devops.infra.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.choerodon.devops.api.vo.BaseApplicationServiceVO;
import io.choerodon.devops.api.vo.iam.DevopsEnvMessageVO;
import io.choerodon.devops.infra.dto.DevopsEnvAppServiceDTO;
import io.choerodon.mybatis.common.Mapper;

public interface DevopsEnvAppServiceMapper extends Mapper<DevopsEnvAppServiceDTO> {

    /**
     * 当记录不存在时，插入记录
     *
     * @param devopsEnvAppServiceDTO 环境应用关联关系
     */
    void insertIgnore(DevopsEnvAppServiceDTO devopsEnvAppServiceDTO);

    /**
     * 通过环境Id查询所有应用Id
     *
     * @param envId 环境Id
     * @return List 应用Id
     */
    List<Long> queryAppByEnvId(@Param("envId") Long envId);

    /**
     * 通过环境Id查询所有应用Id
     *
     * @param envId        环境Id
     * @param appServiceId 应用Id
     * @return List 环境资源信息
     */
    List<DevopsEnvMessageVO> listResourceByEnvAndApp(@Param("envId") Long envId, @Param("appServiceId") Long appServiceId);

    /**
     * 查询项目下可用的且没有与该环境关联的应用
     *
     * @param projectId 项目id
     * @param envId     环境id
     * @return 应用列表
     */
    List<BaseApplicationServiceVO> listNonRelatedApplications(@Param("projectId") Long projectId, @Param("envId") Long envId);

    int countInstances(@Param("appServiceId") Long appServiceId, @Param("envId") Long envId);

    int countRelatedService(@Param("appServiceId") Long appServiceId, @Param("envId") Long envId);

    int countRelatedSecret(@Param("appServiceId") Long appServiceId, @Param("envId") Long envId);

    int countRelatedIngress(@Param("appServiceId") Long appServiceId, @Param("envId") Long envId);

    int countRelatedConfigMap(@Param("appServiceId") Long appServiceId, @Param("envId") Long envId);
}