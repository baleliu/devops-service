package io.choerodon.devops.infra.feign.operator;

import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.exception.ExceptionResponse;
import io.choerodon.core.exception.FeignException;
import io.choerodon.devops.api.vo.OrganizationSimplifyVO;
import io.choerodon.devops.api.vo.RoleAssignmentSearchVO;
import io.choerodon.devops.api.vo.iam.AppDownloadDevopsReqVO;
import io.choerodon.devops.api.vo.iam.ProjectWithRoleVO;
import io.choerodon.devops.api.vo.iam.RemoteTokenAuthorizationVO;
import io.choerodon.devops.api.vo.iam.UserWithRoleVO;
import io.choerodon.devops.api.vo.kubernetes.ProjectCreateDTO;
import io.choerodon.devops.infra.dto.iam.*;
import io.choerodon.devops.infra.enums.OrgPublishMarketStatus;
import io.choerodon.devops.infra.feign.BaseServiceClient;
import io.choerodon.devops.infra.util.FeignParamUtils;
import io.choerodon.devops.infra.util.TypeUtil;
import io.choerodon.mybatis.autoconfigure.CustomPageRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static io.choerodon.core.iam.InitRoleCode.PROJECT_MEMBER;
import static io.choerodon.core.iam.InitRoleCode.PROJECT_OWNER;

/**
 * Created by Sheep on 2019/7/11.
 */

@Component
public class BaseServiceClientOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseServiceClientOperator.class);
    private static final Gson gson = new Gson();

    private static final String LOGIN_NAME = "loginName";
    private static final String REAL_NAME = "realName";


    @Autowired
    private BaseServiceClient baseServiceClient;

    public ProjectDTO queryIamProjectById(Long projectId) {
        ResponseEntity<ProjectDTO> projectDTOResponseEntity = baseServiceClient.queryIamProject(projectId);
        if (!projectDTOResponseEntity.getStatusCode().is2xxSuccessful()) {
            throw new CommonException("error.project.query.by.id", projectId);
        }
        return projectDTOResponseEntity.getBody();
    }

    public OrganizationDTO queryOrganizationById(Long organizationId) {
        ResponseEntity<OrganizationDTO> organizationDTOResponseEntity = baseServiceClient.queryOrganizationById(organizationId);
        if (organizationDTOResponseEntity.getStatusCode().is2xxSuccessful()) {
            return organizationDTOResponseEntity.getBody();
        } else {
            throw new CommonException("error.organization.get");
        }
    }

    public List<ProjectDTO> listIamProjectByOrgId(Long organizationId) {
        return listIamProjectByOrgId(organizationId, null, null, null);
    }


    public List<ProjectDTO> listIamProjectByOrgId(Long organizationId, String name, String code, String params) {
        CustomPageRequest customPageRequest = CustomPageRequest.of(0, 0);
        ResponseEntity<PageInfo<ProjectDTO>> pageResponseEntity =
                baseServiceClient.pageProjectsByOrgId(organizationId, FeignParamUtils.encodePageRequest(customPageRequest), name, code, true, params);
        return Objects.requireNonNull(pageResponseEntity.getBody()).getList();
    }

    public PageInfo<ProjectDTO> pageProjectByOrgId(Long organizationId, int page, int size, Sort sort, String name, String code, String params) {
        CustomPageRequest pageable = CustomPageRequest.of(page, size, sort == null ? Sort.unsorted() : sort);
        try {
            ResponseEntity<PageInfo<ProjectDTO>> pageInfoResponseEntity = baseServiceClient.pageProjectsByOrgId(organizationId,
                    FeignParamUtils.encodePageRequest(pageable), name, code, true, params);
            return pageInfoResponseEntity.getBody();
        } catch (FeignException e) {
            throw new CommonException(e);
        }
    }

    public List<ApplicationDTO> listAppsByOrgId(Long orgId, String name) {
        ResponseEntity<List<ApplicationDTO>> apps;
        try {
            apps = baseServiceClient.queryAppsByOrgId(orgId, false, 1, 0, name, null, null, null);
        } catch (Exception e) {
            throw new CommonException(e);
        }
        return apps.getBody();
    }

    public List<ProjectWithRoleVO> listProjectWithRoleDTO(Long userId) {
        List<ProjectWithRoleVO> returnList = new ArrayList<>();
        int page = 0;
        int size = 0;
        ResponseEntity<PageInfo<ProjectWithRoleVO>> pageResponseEntity =
                baseServiceClient.listProjectWithRole(userId, page, size);
        PageInfo<ProjectWithRoleVO> projectWithRoleDTOPage = pageResponseEntity.getBody();
        if (!projectWithRoleDTOPage.getList().isEmpty()) {
            returnList.addAll(projectWithRoleDTOPage.getList());
        }
        return returnList;
    }

    public List<IamUserDTO> listUsersByIds(List<Long> ids) {
        List<IamUserDTO> userDTOS = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            Long[] newIds = new Long[ids.size()];
            try {
                userDTOS = baseServiceClient
                        .listUsersByIds(ids.toArray(newIds), false).getBody();
            } catch (Exception e) {
                throw new CommonException("error.users.get", e);
            }
        }
        return userDTOS;
    }

    public IamUserDTO queryUserByUserId(Long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        List<IamUserDTO> userES = this.listUsersByIds(ids);
        if (userES != null && !userES.isEmpty()) {
            return userES.get(0);
        }
        return null;
    }

    public List<IamUserDTO> queryUsersByUserIds(List<Long> ids) {
        return this.listUsersByIds(ids);
    }

    public PageInfo<IamUserDTO> pagingQueryUsersByRoleIdOnProjectLevel(Pageable pageable,
                                                                       RoleAssignmentSearchVO roleAssignmentSearchVO,
                                                                       Long roleId, Long projectId, Boolean doPage) {
        try {
            return baseServiceClient
                    .pagingQueryUsersByRoleIdOnProjectLevel(pageable.getPageNumber(), pageable.getPageSize(), roleId,
                            projectId, doPage, roleAssignmentSearchVO).getBody();
        } catch (FeignException e) {
            LOGGER.error("get users by role id {} and project id {} error", roleId, projectId);
        }
        return null;
    }

    public PageInfo<UserWithRoleVO> queryUserPermissionByProjectId(Long projectId, Pageable pageable,
                                                                   Boolean doPage) {
        try {
            RoleAssignmentSearchVO roleAssignmentSearchVO = new RoleAssignmentSearchVO();
            ResponseEntity<PageInfo<UserWithRoleVO>> userEPageResponseEntity = baseServiceClient
                    .queryUserByProjectId(projectId,
                            pageable.getPageNumber(), pageable.getPageSize(), doPage, roleAssignmentSearchVO);
            return userEPageResponseEntity.getBody();
        } catch (FeignException e) {
            LOGGER.error("get user permission by project id {} error", projectId);
            return null;
        }
    }

    public IamUserDTO queryByEmail(Long projectId, String email) {
        try {
            ResponseEntity<PageInfo<IamUserDTO>> userDOResponseEntity = baseServiceClient
                    .listUsersByEmail(projectId, 0, 0, email);
            if (userDOResponseEntity.getBody().getList().isEmpty()) {
                return null;
            }
            return userDOResponseEntity.getBody().getList().get(0);
        } catch (FeignException e) {
            LOGGER.error("get user by email {} error", email);
            return null;
        }
    }

    public Long queryRoleIdByCode(String roleCode) {
        try {

            return baseServiceClient.queryRoleIdByCode(roleCode).getBody().getList().get(0).getId();
        } catch (FeignException e) {
            LOGGER.error("get role id by code {} error", roleCode);
            return null;
        }
    }

    public List<Long> getAllMemberIdsWithoutOwner(Long projectId) {
        // 获取项目成员id
        Long memberId = this.queryRoleIdByCode(PROJECT_MEMBER);
        // 获取项目所有者id
        Long ownerId = this.queryRoleIdByCode(PROJECT_OWNER);
        // 项目下所有项目成员
        List<Long> memberIds =

                this.pagingQueryUsersByRoleIdOnProjectLevel(CustomPageRequest.of(0, 0), new RoleAssignmentSearchVO(), memberId,
                        projectId, false).getList().stream().map(IamUserDTO::getId).collect(Collectors.toList());
        // 项目下所有项目所有者
        List<Long> ownerIds =
                this.pagingQueryUsersByRoleIdOnProjectLevel(CustomPageRequest.of(0, 0), new RoleAssignmentSearchVO(), ownerId,

                        projectId, false).getList().stream().map(IamUserDTO::getId).collect(Collectors.toList());
        return memberIds.stream().filter(e -> !ownerIds.contains(e)).collect(Collectors.toList());
    }

    //获得所有项目所有者id
    public List<Long> getAllOwnerIds(Long projectId) {
        // 获取项目所有者角色id
        Long ownerId = this.queryRoleIdByCode(PROJECT_OWNER);

        // 项目下所有项目所有者
        return this.pagingQueryUsersByRoleIdOnProjectLevel(CustomPageRequest.of(0, 0), new RoleAssignmentSearchVO(), ownerId,
                projectId, false).getList().stream().filter(IamUserDTO::getEnabled).map(IamUserDTO::getId).collect(Collectors.toList());
    }

    public List<IamUserDTO> getAllMember(Long projectId, String params) {
        // 获取项目成员id
        Long memberId = this.queryRoleIdByCode(PROJECT_MEMBER);
        // 获取项目所有者id
        Long ownerId = this.queryRoleIdByCode(PROJECT_OWNER);
        // 项目下所有项目成员

        RoleAssignmentSearchVO roleAssignmentSearchVO = new RoleAssignmentSearchVO();
        roleAssignmentSearchVO.setEnabled(true);
        Map<String, Object> searchParamMap;
        List<String> paramList;
        // 处理搜索参数
        if (!StringUtils.isEmpty(params)) {
            Map maps = gson.fromJson(params, Map.class);
            searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
            paramList = TypeUtil.cast(maps.get(TypeUtil.PARAMS));
            roleAssignmentSearchVO.setParam(paramList == null ? null : paramList.toArray(new String[0]));
            if (searchParamMap != null) {
                if (searchParamMap.get(LOGIN_NAME) != null) {
                    String loginName = TypeUtil.objToString(searchParamMap.get(LOGIN_NAME));
                    roleAssignmentSearchVO.setLoginName(loginName);
                }
                if (searchParamMap.get(REAL_NAME) != null) {
                    String realName = TypeUtil.objToString(searchParamMap.get(REAL_NAME));
                    roleAssignmentSearchVO.setRealName(realName);
                }
            }
        }

        // 查出项目下的所有成员
        List<IamUserDTO> list = this.pagingQueryUsersByRoleIdOnProjectLevel(CustomPageRequest.of(0, 0), roleAssignmentSearchVO, memberId,
                projectId, false).getList();
        List<Long> memberIds = list.stream().filter(IamUserDTO::getEnabled).map(IamUserDTO::getId).collect(Collectors.toList());
        // 项目下所有项目所有者
        this.pagingQueryUsersByRoleIdOnProjectLevel(CustomPageRequest.of(0, 0), roleAssignmentSearchVO, ownerId,
                projectId, false).getList().stream().filter(IamUserDTO::getEnabled).forEach(t -> {
            if (!memberIds.contains(t.getId())) {
                list.add(t);
            }
        });
        return list;
    }

    public Boolean isProjectOwner(Long userId, Long projectId) {
        Boolean isProjectOwner;
        try {
            isProjectOwner = baseServiceClient.checkIsProjectOwner(userId, projectId).getBody();
        } catch (FeignException e) {
            throw new CommonException(e);
        }
        return isProjectOwner;
    }

    public IamAppDTO createIamApp(Long organizationId, IamAppDTO appDTO) {
        ResponseEntity<IamAppDTO> appDTOResponseEntity = null;
        try {
            appDTOResponseEntity = baseServiceClient.createIamApplication(organizationId, appDTO);
        } catch (FeignException e) {
            throw new CommonException(e);
        }
        IamAppDTO result = appDTOResponseEntity.getBody();
        if (result == null || result.getProjectId() == null) {
            throw new CommonException("error.code.exist");
        }
        return result;
    }

    public IamAppDTO updateIamApp(Long organizationId, Long id, IamAppDTO appDTO) {
        ResponseEntity<IamAppDTO> appDTOResponseEntity = null;
        try {
            appDTOResponseEntity = baseServiceClient.updateIamApplication(organizationId, id, appDTO);
        } catch (FeignException e) {
            throw new CommonException(e);
        }
        return appDTOResponseEntity.getBody();
    }

    public IamAppDTO queryIamAppByCode(Long organizationId, String code) {
        ResponseEntity<PageInfo<IamAppDTO>> pageInfoResponseEntity = null;
        try {
            pageInfoResponseEntity = baseServiceClient.getIamApplication(organizationId, code);
        } catch (FeignException e) {
            throw new CommonException(e);
        }
        return pageInfoResponseEntity.getBody().getList().isEmpty() ? null : pageInfoResponseEntity.getBody().getList().get(0);
    }

    public ProjectDTO createProject(Long organizationId, ProjectCreateDTO projectCreateDTO) {
        try {
            ResponseEntity<ProjectDTO> projectDTOResponseEntity = baseServiceClient
                    .createProject(organizationId, projectCreateDTO);
            return projectDTOResponseEntity.getBody();
        } catch (FeignException e) {
            LOGGER.error("error.create.iam.project");
            return null;
        }
    }

    public PageInfo<OrganizationSimplifyVO> getAllOrgs(Integer page, Integer size) {
        try {
            ResponseEntity<PageInfo<OrganizationSimplifyVO>> simplifyDTOs = baseServiceClient
                    .getAllOrgs(page, size);
            return simplifyDTOs.getBody();
        } catch (FeignException e) {
            LOGGER.error("error.get.all.organization");
            return null;
        }
    }

    public ProjectDTO queryProjectByAppId(Long id) {
        try {
            return baseServiceClient.queryProjectByAppId(id).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public ApplicationDTO queryAppById(Long id) {
        try {
            return baseServiceClient.queryAppById(id).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public List<ApplicationDTO> getAppByIds(Set<Long> ids) {
        try {
            return baseServiceClient.getAppByIds(ids).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public void publishFail(Long projectId, Long mktAppVersionId, String code, Boolean fixFlag) {
        try {
            baseServiceClient.publishFail(projectId, mktAppVersionId, code, fixFlag).getBody();
        } catch (Exception e) {
            throw new CommonException("error.insert.failed.message", e.getMessage());
        }
    }

    public PageInfo<ProjectDTO> pagingProjectByOptions(Long organizationId, int page, int size, String[] params) {
        try {
            return baseServiceClient.pagingProjectByOptions(organizationId, false, page, size, params).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public void completeDownloadApplication(Long publishAppVersionId, Long appVersionId, Long organizationId, List<AppDownloadDevopsReqVO> appDownloadDevopsReqVOS) {
        try {
            ResponseEntity<String> responseEntity = baseServiceClient.completeDownloadApplication(publishAppVersionId, appVersionId, organizationId, appDownloadDevopsReqVOS);
            if (responseEntity != null && responseEntity.getBody() != null) {
                ExceptionResponse exceptionResponse = gson.fromJson(responseEntity.getBody(), ExceptionResponse.class);
                if (exceptionResponse.getFailed()) {
                    throw new CommonException("error.application.download.complete");
                }
            }
        } catch (Exception e) {
            throw new CommonException("error.application.download.complete", e.getMessage());
        }
    }

    public void failToDownloadApplication(Long publishAppVersionId, Long appVersionId, Long organizationId) {
        try {
            ResponseEntity<String> responseEntity = baseServiceClient.failToDownloadApplication(publishAppVersionId, appVersionId, organizationId);
            if (responseEntity != null && responseEntity.getBody() != null) {
                ExceptionResponse exceptionResponse = gson.fromJson(responseEntity.getBody(), ExceptionResponse.class);
                if (exceptionResponse.getFailed()) {
                    throw new CommonException("error.application.download.failed");
                }
            }
        } catch (Exception e) {
            throw new CommonException("error.application.download.failed", e.getMessage());
        }
    }

    public String checkLatestToken() {
        try {
            RemoteTokenAuthorizationVO remoteTokenAuthorizationVO = baseServiceClient.checkLatestToken().getBody();
            if (remoteTokenAuthorizationVO != null) {
                return remoteTokenAuthorizationVO.getRemoteToken();
            }
        } catch (Exception e) {
            throw new CommonException("error.remote.token.authorization", e.getMessage());
        }
        return null;
    }

    public List<ProjectDTO> queryProjectsByIds(Set<Long> ids) {
        try {
            return baseServiceClient.queryByIds(ids).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public List<ApplicationDTO> listApplicationInfoByAppIds(Long projectId, Set<Long> serviceIds) {
        try {
            return baseServiceClient.listApplicationInfoByAppIds(projectId, serviceIds).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Set<Long> listAppServiceByAppId(Long projectId, Long appId) {
        try {
            return baseServiceClient.listAppServiceByAppId(projectId, appId).getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据组织id和项目code查询项目
     *
     * @param projectCode    项目code
     * @param organizationId 组织id
     * @return 项目信息(可能为空)
     */
    @Nullable
    public ProjectDTO queryProjectByCodeAndOrganizationId(@Nonnull String projectCode, @Nonnull Long organizationId) {
        try {
            ResponseEntity<ProjectDTO> resp = baseServiceClient.queryProjectByCodeAndOrgId(organizationId, projectCode);
            return resp == null ? null : resp.getBody();
        } catch (Exception ex) {
            LOGGER.info("Exception occurred when querying project by code {} and organization id {}", projectCode, organizationId);
            return null;
        }
    }

    public List<Long> listServicesForMarket(@Nonnull Long organizationId, Boolean deployOnly) {
        String status = deployOnly != null && deployOnly ? OrgPublishMarketStatus.DEPLOY_ONLY.getType() : OrgPublishMarketStatus.DOWNLOAD_ONLY.getType();
        try {
            ResponseEntity<Set<Long>> resp = baseServiceClient.listService(organizationId, status);

            return resp == null || resp.getBody() == null ? null : new ArrayList<>(resp.getBody());
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Long> listServiceVersionsForMarket(@Nonnull Long organizationId, Boolean deployOnly) {
        String status = deployOnly != null && deployOnly ? OrgPublishMarketStatus.DEPLOY_ONLY.getType() : OrgPublishMarketStatus.DOWNLOAD_ONLY.getType();
        try {
            ResponseEntity<Set<Long>> resp = baseServiceClient.listSvcVersion(organizationId, status);
            return resp == null || resp.getBody() == null ? null : new ArrayList<>(resp.getBody());
        } catch (Exception ex) {
            return null;
        }
    }

    public List<IamUserDTO> listAllOrganizationOwners(Long organizationId) {
        try {
            ResponseEntity<PageInfo<IamUserDTO>> users = baseServiceClient.pagingQueryUsersWithRolesOnOrganizationLevel(organizationId, 0, 0, null, null, "组织管理员", true, false, null);
            return (users == null || users.getBody() == null) ? Collections.emptyList() : users.getBody().getList();
        } catch (Exception ex) {
            LOGGER.info("Exception occurred when listing organization owners of organization with id {}", organizationId);
            return Collections.emptyList();
        }
    }

    /**
     * 项目层批量分配权限
     */
    public void assignProjectOwnerForUsersInProject(
            Long projectId,
            Set<Long> userIds,
            Long projectOwnerId) {
        List<MemberRoleDTO> memberRoleDTOS = userIds.stream().map(userId -> {
            MemberRoleDTO memberRoleDTO = new MemberRoleDTO();
            memberRoleDTO.setMemberId(userId);
            memberRoleDTO.setMemberType("user");
            memberRoleDTO.setSourceId(projectId);
            memberRoleDTO.setSourceType("project");
            memberRoleDTO.setRoleId(projectOwnerId);
            return memberRoleDTO;
        }).collect(Collectors.toList());
        baseServiceClient.assignUsersRolesOnProjectLevel(projectId, memberRoleDTOS);
    }

    public List<ProjectWithRoleVO> listProjectWithRole(Long userId, int page, int size) {
        try {
            ResponseEntity<PageInfo<ProjectWithRoleVO>> pageInfoResponseEntity = baseServiceClient.listProjectWithRole(userId, page, size);
            return (pageInfoResponseEntity.getBody() == null) ? Collections.emptyList() : pageInfoResponseEntity.getBody().getList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }

    }

    public ClientDTO createClient(Long organizationId, ClientVO clientVO) {
        try {
            ClientDTO client = baseServiceClient.createClient(organizationId, clientVO).getBody();
            if (client == null) {
                throw new CommonException("error.create.client");
            }
            return client;
        } catch (Exception ex) {
            throw new CommonException("error.create.client");
        }
    }

    public void deleteClient(Long organizationId, Long clientId) {
        try {
            baseServiceClient.deleteClient(organizationId, clientId);
        } catch (Exception ex) {
            throw new CommonException("error.delete.client");
        }
    }

    public ClientDTO queryClientBySourceId(Long organizationId, Long sourceId) {
        try {
            ResponseEntity<ClientDTO> responseEntity = baseServiceClient.queryClientBySourceId(organizationId, sourceId);
            return responseEntity.getBody();
        } catch (Exception ex) {
            throw new CommonException("error.query.client");
        }
    }

    public List<IamUserDTO> listProjectUsersByPorjectIdAndRoleLable(Long projectId, String roleLable) {
        try {
            ResponseEntity<List<IamUserDTO>> responseEntity = baseServiceClient.listProjectUsersByPorjectIdAndRoleLable(projectId, roleLable);
            return responseEntity.getBody();
        } catch (Exception ex) {
            throw new CommonException("error.query.project.users");
        }
    }

    /**
     * 通过登录名查询用户
     *
     * @param loginName 登录名
     * @return 用户信息
     */
    public IamUserDTO queryUserByLoginName(String loginName) {
        try {
            ResponseEntity<IamUserDTO> responseEntity = baseServiceClient.queryByLoginName(loginName);
            IamUserDTO iamUserDTO = responseEntity.getBody();
            if (iamUserDTO == null || iamUserDTO.getId() == null) {
                throw new CommonException("error.query.user.by.login.name", loginName);
            }
            return iamUserDTO;
        } catch (Exception ex) {
            throw new CommonException("error.query.user.by.login.name", loginName);
        }
    }

    public List<IamUserDTO> queryAllRootUsers() {
        ResponseEntity<List<IamUserDTO>> responseEntity = baseServiceClient.queryAllAdminUsers();
        return responseEntity == null ? Collections.emptyList() : responseEntity.getBody();
    }
}
