package dev.eyadsharkawy.agency_os_api.global.workspace.repository;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.InvitationStatus;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {
  List<WorkspaceInvitation> findByUsernameIgnoreCaseAndStatus(
      String username, InvitationStatus status);

  Optional<WorkspaceInvitation> findByWorkspaceIdAndUsernameIgnoreCase(
      UUID workspaceId, String username);
}
