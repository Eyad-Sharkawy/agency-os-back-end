package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.user.service.UserSyncService;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceMemberResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceMemberUpdateRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspaceId;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.event.WorkspaceCreatedEvent;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private AppUserRepository userRepository;
  @Mock private UserSyncService userSyncService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private UserWorkspaceRepository userWorkspaceRepository;
  @Mock private ClientUserRepository clientUserRepository;
  @Mock private ClientRepository clientRepository;

  @InjectMocks private WorkspaceService workspaceService;

  private Jwt jwt;
  private AppUser ownerUser;
  private AppUser memberUser;
  private Workspace workspace;

  @BeforeEach
  void setUp() {
    jwt = mock(Jwt.class);
    lenient().when(jwt.getSubject()).thenReturn("kc-user-123");

    ownerUser = new AppUser();
    ownerUser.setId(UUID.randomUUID());
    ownerUser.setKeycloakId("kc-user-123");
    ownerUser.setUsername("testuser");
    ownerUser.setEmail("owner@agency.com");
    ownerUser.setUserWorkspaces(new HashSet<>());

    memberUser = new AppUser();
    memberUser.setId(UUID.randomUUID());
    memberUser.setKeycloakId("kc-member-456");
    memberUser.setUsername("memberuser");
    memberUser.setEmail("member@agency.com");
    memberUser.setUserWorkspaces(new HashSet<>());

    workspace = new Workspace();
    workspace.setId(UUID.randomUUID());
    workspace.setName("Acme Agency");
    workspace.setTenantId("tenant_acme");
    workspace.setContactEmail("user@agency.com");
    workspace.setUserWorkspaces(new HashSet<>());
  }

  @Test
  @DisplayName("createWorkspace should create workspace, add owner membership, and publish event")
  void createWorkspace_Success_ExistingUser() {
    WorkspaceRequest request = new WorkspaceRequest("Acme Agency");

    when(userSyncService.getOrSyncUser(jwt)).thenReturn(ownerUser);
    when(workspaceRepository.existsByTenantId(anyString())).thenReturn(false);
    when(workspaceRepository.save(any(Workspace.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    WorkspaceResponse response = workspaceService.createWorkspace(jwt, request);

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Acme Agency");
    assertThat(response.role()).isEqualTo("OWNER");

    verify(eventPublisher, times(1)).publishEvent(any(WorkspaceCreatedEvent.class));
    verify(workspaceRepository, times(1)).save(any(Workspace.class));
    verify(userRepository, times(1)).save(ownerUser);
  }

  @Test
  @DisplayName("createWorkspace should sync user from JWT if not found in DB")
  void createWorkspace_Success_NewUser() {
    WorkspaceRequest request = new WorkspaceRequest("New Agency");

    when(userSyncService.getOrSyncUser(jwt)).thenReturn(ownerUser);
    when(workspaceRepository.existsByTenantId(anyString())).thenReturn(false);
    when(workspaceRepository.save(any(Workspace.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    WorkspaceResponse response = workspaceService.createWorkspace(jwt, request);

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("New Agency");
    assertThat(response.role()).isEqualTo("OWNER");

    verify(eventPublisher, times(1)).publishEvent(any(WorkspaceCreatedEvent.class));
  }

  @Test
  @DisplayName("getUserWorkspaces should return user workspaces")
  void getUserWorkspaces_Success() {
    UserWorkspace uw = new UserWorkspace();
    uw.setUser(ownerUser);
    uw.setWorkspace(workspace);
    uw.setRole(WorkspaceRole.OWNER);
    ownerUser.getUserWorkspaces().add(uw);

    when(userRepository.findByKeycloakId("kc-user-123")).thenReturn(Optional.of(ownerUser));

    List<WorkspaceResponse> responses = workspaceService.getUserWorkspaces(jwt);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).name()).isEqualTo("Acme Agency");
    assertThat(responses.get(0).role()).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("getUserWorkspaces should return empty list if user not found")
  void getUserWorkspaces_UserNotFound() {
    when(userRepository.findByKeycloakId("kc-user-123")).thenReturn(Optional.empty());

    List<WorkspaceResponse> responses = workspaceService.getUserWorkspaces(jwt);

    assertThat(responses).isEmpty();
  }

  @Test
  @DisplayName("deleteUserWorkspaceByTenantId should delete workspace when found")
  void deleteUserWorkspaceByTenantId_Success() {
    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));

    workspaceService.deleteUserWorkspaceByTenantId("tenant_acme");

    verify(workspaceRepository, times(1)).delete(workspace);
  }

  @Test
  @DisplayName("deleteUserWorkspaceByTenantId should throw exception when workspace not found")
  void deleteUserWorkspaceByTenantId_NotFound() {
    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workspaceService.deleteUserWorkspaceByTenantId("tenant_acme"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("updateUserWorkspaceByTenantId should update name and return response")
  void updateUserWorkspaceByTenantId_Success() {
    WorkspaceRequest request = new WorkspaceRequest("Updated Name");
    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

    WorkspaceResponse response =
        workspaceService.updateUserWorkspaceByTenantId("tenant_acme", request);

    assertThat(response.name()).isEqualTo("Updated Name");
    verify(workspaceRepository, times(1)).save(workspace);
  }

  @Test
  @DisplayName("getWorkspaceMembers should return list of member responses")
  void getWorkspaceMembers_Success() {
    UserWorkspace uw = new UserWorkspace();
    uw.setUser(ownerUser);
    uw.setWorkspace(workspace);
    uw.setRole(WorkspaceRole.OWNER);
    workspace.getUserWorkspaces().add(uw);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));

    List<WorkspaceMemberResponse> members = workspaceService.getWorkspaceMembers("tenant_acme");

    assertThat(members).hasSize(1);
    assertThat(members.get(0).username()).isEqualTo("testuser");
    assertThat(members.get(0).role()).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("updateWorkspaceMember should throw exception when modifying own role")
  void updateWorkspaceMember_CannotModifyOwnRole() {
    UserWorkspace uwOwner = new UserWorkspace();
    uwOwner.setUser(ownerUser);
    uwOwner.setWorkspace(workspace);
    uwOwner.setRole(WorkspaceRole.OWNER);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.OWNER));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(ownerUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(uwOwner));

    WorkspaceMemberUpdateRequest request =
        new WorkspaceMemberUpdateRequest(WorkspaceRole.ADMIN, null);
    UUID ownerId = ownerUser.getId();

    assertThatThrownBy(
            () -> workspaceService.updateWorkspaceMember(jwt, "tenant_acme", ownerId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("You cannot modify your own role");
  }

  @Test
  @DisplayName("updateWorkspaceMember should throw exception when setting role to OWNER")
  void updateWorkspaceMember_CannotSetOwnerRole() {
    UserWorkspace uwMember = new UserWorkspace();
    uwMember.setUser(memberUser);
    uwMember.setWorkspace(workspace);
    uwMember.setRole(WorkspaceRole.MEMBER);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.OWNER));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(memberUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(uwMember));

    WorkspaceMemberUpdateRequest request =
        new WorkspaceMemberUpdateRequest(WorkspaceRole.OWNER, null);
    UUID memberId = memberUser.getId();

    assertThatThrownBy(
            () -> workspaceService.updateWorkspaceMember(jwt, "tenant_acme", memberId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ownership transfer flow");
  }

  @Test
  @DisplayName(
      "updateWorkspaceMember should throw AccessDeniedException if Admin tries to assign Admin role")
  void updateWorkspaceMember_AdminCannotPromoteToAdmin() {
    UserWorkspace uwMember = new UserWorkspace();
    uwMember.setUser(memberUser);
    uwMember.setWorkspace(workspace);
    uwMember.setRole(WorkspaceRole.MEMBER);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.ADMIN));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(memberUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(uwMember));

    WorkspaceMemberUpdateRequest request =
        new WorkspaceMemberUpdateRequest(WorkspaceRole.ADMIN, null);
    UUID memberId = memberUser.getId();

    assertThatThrownBy(
            () -> workspaceService.updateWorkspaceMember(jwt, "tenant_acme", memberId, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Admins cannot promote users");
  }

  @Test
  @DisplayName("updateWorkspaceMember to CLIENT should save ClientUser")
  void updateWorkspaceMember_ToClient_Success() {
    UUID clientId = UUID.randomUUID();
    Client client = new Client();
    client.setId(clientId);

    UserWorkspace uwMember = new UserWorkspace();
    uwMember.setUser(memberUser);
    uwMember.setWorkspace(workspace);
    uwMember.setRole(WorkspaceRole.MEMBER);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.OWNER));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(memberUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(uwMember));
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

    WorkspaceMemberUpdateRequest request =
        new WorkspaceMemberUpdateRequest(WorkspaceRole.CLIENT, clientId);

    workspaceService.updateWorkspaceMember(jwt, "tenant_acme", memberUser.getId(), request);

    assertThat(uwMember.getRole()).isEqualTo(WorkspaceRole.CLIENT);
    verify(clientUserRepository, times(1)).save(any(ClientUser.class));
    verify(userWorkspaceRepository, times(1)).save(uwMember);
  }

  @Test
  @DisplayName("removeWorkspaceMember should throw exception on self-removal")
  void removeWorkspaceMember_SelfRemoval_ThrowsException() {
    UserWorkspace uwOwner = new UserWorkspace();
    uwOwner.setUser(ownerUser);
    uwOwner.setWorkspace(workspace);
    uwOwner.setRole(WorkspaceRole.OWNER);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.OWNER));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(ownerUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(uwOwner));

    UUID ownerId = ownerUser.getId();

    assertThatThrownBy(
            () -> workspaceService.removeWorkspaceMember("kc-user-123", "tenant_acme", ownerId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot remove yourself");
  }

  @Test
  @DisplayName("removeWorkspaceMember should delete member workspace association")
  void removeWorkspaceMember_Success() {
    UserWorkspace uwMember = new UserWorkspace();
    uwMember.setUser(memberUser);
    uwMember.setWorkspace(workspace);
    uwMember.setRole(WorkspaceRole.MEMBER);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.OWNER));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(memberUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(uwMember));

    workspaceService.removeWorkspaceMember("kc-user-123", "tenant_acme", memberUser.getId());

    verify(userWorkspaceRepository, times(1)).delete(uwMember);
  }

  @Test
  @DisplayName("transferOwnership should update roles of old and new owner")
  void transferOwnership_Success() {
    UserWorkspace oldOwnerUw = new UserWorkspace();
    oldOwnerUw.setUser(ownerUser);
    oldOwnerUw.setWorkspace(workspace);
    oldOwnerUw.setRole(WorkspaceRole.OWNER);

    UserWorkspace newOwnerUw = new UserWorkspace();
    newOwnerUw.setUser(memberUser);
    newOwnerUw.setWorkspace(workspace);
    newOwnerUw.setRole(WorkspaceRole.MEMBER);

    workspace.getUserWorkspaces().add(oldOwnerUw);
    workspace.getUserWorkspaces().add(newOwnerUw);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(ownerUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(oldOwnerUw));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(memberUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(newOwnerUw));

    workspaceService.transferOwnership("kc-user-123", "tenant_acme", memberUser.getId());

    assertThat(newOwnerUw.getRole()).isEqualTo(WorkspaceRole.OWNER);
    assertThat(oldOwnerUw.getRole()).isEqualTo(WorkspaceRole.ADMIN);
    verify(userWorkspaceRepository, times(1)).save(newOwnerUw);
    verify(userWorkspaceRepository, times(1)).save(oldOwnerUw);
  }

  @Test
  @DisplayName(
      "updateWorkspaceMember should throw AccessDeniedException when non-OWNER modifies CLIENT role")
  void updateWorkspaceMember_NonOwnerModifiesClientRole_AccessDenied() {
    UserWorkspace uwMember = new UserWorkspace();
    uwMember.setUser(memberUser);
    uwMember.setWorkspace(workspace);
    uwMember.setRole(WorkspaceRole.MEMBER);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.ADMIN));
    when(userWorkspaceRepository.findById(
            new UserWorkspaceId(memberUser.getId(), workspace.getId())))
        .thenReturn(Optional.of(uwMember));

    WorkspaceMemberUpdateRequest request =
        new WorkspaceMemberUpdateRequest(WorkspaceRole.CLIENT, UUID.randomUUID());
    UUID memberId = memberUser.getId();

    assertThatThrownBy(
            () -> workspaceService.updateWorkspaceMember(jwt, "tenant_acme", memberId, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only the workspace OWNER can modify CLIENT role associations");
  }

  @Test
  @DisplayName(
      "updateWorkspaceMember should throw ResourceNotFoundException when requester role not found")
  void updateWorkspaceMember_RequesterRoleNotFound_ThrowsException() {
    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.empty());

    WorkspaceMemberUpdateRequest request =
        new WorkspaceMemberUpdateRequest(WorkspaceRole.MEMBER, null);
    UUID memberId = memberUser.getId();

    assertThatThrownBy(
            () -> workspaceService.updateWorkspaceMember(jwt, "tenant_acme", memberId, request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Workspace not found: tenant_acme");
  }

  @Test
  @DisplayName(
      "removeWorkspaceMember should throw ResourceNotFoundException when workspace not found")
  void removeWorkspaceMember_WorkspaceNotFound_ThrowsException() {
    when(workspaceRepository.findByTenantId("tenant_unknown")).thenReturn(Optional.empty());
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(
            () -> workspaceService.removeWorkspaceMember("kc-user-123", "tenant_unknown", userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Workspace not found: tenant_unknown");
  }

  @Test
  @DisplayName("transferOwnership should throw ResourceNotFoundException when workspace not found")
  void transferOwnership_WorkspaceNotFound_ThrowsException() {
    when(workspaceRepository.findByTenantId("tenant_unknown")).thenReturn(Optional.empty());
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(
            () -> workspaceService.transferOwnership("kc-user-123", "tenant_unknown", userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Workspace not found: tenant_unknown");
  }
}
