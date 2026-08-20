package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.InvitationStatus;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceInvitation;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceInvitationRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTest {

  @Mock private WorkspaceInvitationRepository invitationRepository;
  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private AppUserRepository userRepository;
  @Mock private UserWorkspaceRepository userWorkspaceRepository;
  @Mock private ClientUserRegistrationService clientUserRegistrationService;

  @InjectMocks private WorkspaceInvitationService invitationService;

  private Jwt inviterJwt;
  private Workspace workspace;
  private AppUser inviteeUser;

  @BeforeEach
  void setUp() {
    inviterJwt = mock(Jwt.class);
    lenient().when(inviterJwt.getSubject()).thenReturn("kc-inviter-123");
    lenient().when(inviterJwt.getClaimAsString("preferred_username")).thenReturn("admin_user");

    workspace = new Workspace();
    workspace.setId(UUID.randomUUID());
    workspace.setName("Acme Agency");
    workspace.setTenantId("tenant_acme");

    inviteeUser = new AppUser();
    inviteeUser.setId(UUID.randomUUID());
    inviteeUser.setKeycloakId("kc-invitee-456");
    inviteeUser.setUsername("jane_doe");
    inviteeUser.setEmail("jane@example.com");
  }

  @Test
  @DisplayName("inviteUser should throw exception if neither email nor username is provided")
  void inviteUser_MissingUsernameAndEmail() {
    WorkspaceInvitationRequest request =
        new WorkspaceInvitationRequest(null, null, WorkspaceRole.MEMBER, null);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));

    assertThatThrownBy(() -> invitationService.inviteUser(inviterJwt, "tenant_acme", request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Either username or username must be provided");
  }

  @Test
  @DisplayName(
      "inviteUser should throw AccessDeniedException if non-OWNER tries to invite non-MEMBER role")
  void inviteUser_NonOwnerInvitingAdmin_AccessDenied() {
    WorkspaceInvitationRequest request =
        new WorkspaceInvitationRequest(null, "jane_doe", WorkspaceRole.ADMIN, null);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userRepository.findByUsername("jane_doe")).thenReturn(Optional.of(inviteeUser));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-inviter-123", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.MEMBER));

    assertThatThrownBy(() -> invitationService.inviteUser(inviterJwt, "tenant_acme", request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only workspace OWNER can invite non MEMBER users");
  }

  @Test
  @DisplayName("inviteUser should throw IllegalArgumentException if user is already a member")
  void inviteUser_AlreadyMember() {
    WorkspaceInvitationRequest request =
        new WorkspaceInvitationRequest(null, "jane_doe", WorkspaceRole.MEMBER, null);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userRepository.findByUsername("jane_doe")).thenReturn(Optional.of(inviteeUser));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-invitee-456", "tenant_acme"))
        .thenReturn(Optional.of(WorkspaceRole.MEMBER));

    assertThatThrownBy(() -> invitationService.inviteUser(inviterJwt, "tenant_acme", request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already a member");
  }

  @Test
  @DisplayName("inviteUser should throw IllegalArgumentException if invitation is already pending")
  void inviteUser_AlreadyPending() {
    WorkspaceInvitationRequest request =
        new WorkspaceInvitationRequest(null, "jane_doe", WorkspaceRole.MEMBER, null);

    WorkspaceInvitation pendingInv = new WorkspaceInvitation();
    pendingInv.setStatus(InvitationStatus.PENDING);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userRepository.findByUsername("jane_doe")).thenReturn(Optional.of(inviteeUser));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-invitee-456", "tenant_acme"))
        .thenReturn(Optional.empty());
    when(invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(workspace.getId(), "jane_doe"))
        .thenReturn(Optional.of(pendingInv));

    assertThatThrownBy(() -> invitationService.inviteUser(inviterJwt, "tenant_acme", request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("An invitation is already pending");
  }

  @Test
  @DisplayName("inviteUser should create and save invitation successfully")
  void inviteUser_Success() {
    WorkspaceInvitationRequest request =
        new WorkspaceInvitationRequest("jane@example.com", null, WorkspaceRole.MEMBER, null);

    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(workspace));
    when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(inviteeUser));
    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-invitee-456", "tenant_acme"))
        .thenReturn(Optional.empty());
    when(invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(workspace.getId(), "jane_doe"))
        .thenReturn(Optional.empty());
    when(invitationRepository.save(any(WorkspaceInvitation.class)))
        .thenAnswer(
            i -> {
              WorkspaceInvitation inv = i.getArgument(0);
              inv.setId(UUID.randomUUID());
              return inv;
            });

    WorkspaceInvitationResponse response =
        invitationService.inviteUser(inviterJwt, "tenant_acme", request);

    assertThat(response).isNotNull();
    assertThat(response.username()).isEqualTo("jane_doe");
    assertThat(response.role()).isEqualTo("MEMBER");
    assertThat(response.status()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName("getPendingInvitations should return pending invitations for current user")
  void getPendingInvitations_Success() {
    when(userRepository.findByKeycloakId("kc-inviter-123")).thenReturn(Optional.of(inviteeUser));

    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setId(UUID.randomUUID());
    inv.setWorkspace(workspace);
    inv.setUsername("jane_doe");
    inv.setInvitedByUsername("admin_user");
    inv.setRole(WorkspaceRole.MEMBER);
    inv.setStatus(InvitationStatus.PENDING);

    when(invitationRepository.findByUsernameIgnoreCaseAndStatus(
            "jane_doe", InvitationStatus.PENDING))
        .thenReturn(List.of(inv));

    List<WorkspaceInvitationResponse> pending = invitationService.getPendingInvitations(inviterJwt);

    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).username()).isEqualTo("jane_doe");
  }

  @Test
  @DisplayName("acceptInvitation should throw exception if user username does not match invitation")
  void acceptInvitation_UsernameMismatch() {
    UUID invId = UUID.randomUUID();
    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setId(invId);
    inv.setUsername("different_user");

    when(invitationRepository.findById(invId)).thenReturn(Optional.of(inv));
    when(userRepository.findByKeycloakId("kc-inviter-123")).thenReturn(Optional.of(inviteeUser));

    assertThatThrownBy(() -> invitationService.acceptInvitation(inviterJwt, invId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not authorized to accept");
  }

  @Test
  @DisplayName("acceptInvitation for CLIENT role without clientId should throw exception")
  void acceptInvitation_ClientRoleMissingClientId() {
    UUID invId = UUID.randomUUID();
    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setId(invId);
    inv.setUsername("jane_doe");
    inv.setRole(WorkspaceRole.CLIENT);
    inv.setClientId(null);
    inv.setWorkspace(workspace);

    when(invitationRepository.findById(invId)).thenReturn(Optional.of(inv));
    when(userRepository.findByKeycloakId("kc-inviter-123")).thenReturn(Optional.of(inviteeUser));

    assertThatThrownBy(() -> invitationService.acceptInvitation(inviterJwt, invId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Client ID is required");
  }

  @Test
  @DisplayName(
      "acceptInvitation for CLIENT role with valid client should save ClientUser and accept invitation")
  void acceptInvitation_ClientRole_Success() {
    UUID invId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();

    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setId(invId);
    inv.setUsername("jane_doe");
    inv.setRole(WorkspaceRole.CLIENT);
    inv.setClientId(clientId);
    inv.setWorkspace(workspace);

    when(invitationRepository.findById(invId)).thenReturn(Optional.of(inv));
    when(userRepository.findByKeycloakId("kc-inviter-123")).thenReturn(Optional.of(inviteeUser));

    invitationService.acceptInvitation(inviterJwt, invId);

    assertThat(inv.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    verify(clientUserRegistrationService, times(1)).registerClientUser("kc-invitee-456", clientId);
    verify(invitationRepository, times(1)).save(inv);
  }

  @Test
  @DisplayName("declineInvitation should set status to DECLINED")
  void declineInvitation_Success() {
    UUID invId = UUID.randomUUID();
    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setId(invId);
    inv.setUsername("jane_doe");
    inv.setStatus(InvitationStatus.PENDING);
    inv.setWorkspace(workspace);

    when(invitationRepository.findById(invId)).thenReturn(Optional.of(inv));
    when(userRepository.findByKeycloakId("kc-inviter-123")).thenReturn(Optional.of(inviteeUser));

    invitationService.declineInvitation(inviterJwt, invId);

    assertThat(inv.getStatus()).isEqualTo(InvitationStatus.DECLINED);
    verify(invitationRepository, times(1)).save(inv);
  }
}
