package pk.ls.pasir.sosnowski_lukasz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.ls.pasir.sosnowski_lukasz.dto.MembershipDTO;
import pk.ls.pasir.sosnowski_lukasz.dto.MembershipResponseDTO;
import pk.ls.pasir.sosnowski_lukasz.model.Group;
import pk.ls.pasir.sosnowski_lukasz.model.Membership;
import pk.ls.pasir.sosnowski_lukasz.model.User;
import pk.ls.pasir.sosnowski_lukasz.repository.GroupRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.MembershipRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.UserRepository;

import java.util.List;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public MembershipService(MembershipRepository membershipRepository,
                             GroupRepository groupRepository,
                             UserRepository userRepository,
                             CurrentUserService currentUserService) {
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public List<MembershipResponseDTO> getGroupMembers(Long groupId) {
        assertCurrentUserIsGroupMember(groupId);

        return membershipRepository.findByGroupId(groupId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public MembershipResponseDTO addMember(MembershipDTO membershipDTO) {
        Long groupId = membershipDTO.getGroupId();

        assertCurrentUserIsGroupOwner(groupId);

        Group group = getGroupOrThrow(groupId);

        User userToAdd = userRepository.findByEmail(membershipDTO.getUserEmail())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono użytkownika o emailu: " + membershipDTO.getUserEmail()
                ));

        if (membershipRepository.existsByGroupIdAndUserId(groupId, userToAdd.getId())) {
            throw new IllegalStateException("Użytkownik jest już członkiem tej grupy");
        }

        Membership membership = new Membership();
        membership.setGroup(group);
        membership.setUser(userToAdd);

        Membership savedMembership = membershipRepository.save(membership);

        return mapToResponseDTO(savedMembership);
    }

    public Boolean removeMember(Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono członkostwa o id: " + membershipId
                ));

        Group group = membership.getGroup();

        assertCurrentUserIsGroupOwner(group.getId());

        if (group.getOwner().getId().equals(membership.getUser().getId())) {
            throw new IllegalStateException("Nie można usunąć właściciela z jego grupy");
        }

        membershipRepository.delete(membership);

        return true;
    }

    public void assertCurrentUserIsGroupMember(Long groupId) {
        User currentUser = currentUserService.getCurrentUser();

        if (!membershipRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
            throw new AccessDeniedException("Użytkownik nie jest członkiem tej grupy");
        }
    }

    public void assertCurrentUserIsGroupOwner(Long groupId) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = currentUserService.getCurrentUser();

        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może wykonać tę operację");
        }
    }

    public void assertUserIsGroupMember(Long groupId, Long userId) {
        if (!membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new AccessDeniedException("Wskazany użytkownik nie jest członkiem tej grupy");
        }
    }

    public boolean isCurrentUserGroupOwner(Long groupId) {
        Group group = getGroupOrThrow(groupId);
        User currentUser = currentUserService.getCurrentUser();

        return group.getOwner().getId().equals(currentUser.getId());
    }

    private Group getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o id: " + groupId
                ));
    }

    private MembershipResponseDTO mapToResponseDTO(Membership membership) {
        return new MembershipResponseDTO(
                membership.getId(),
                membership.getUser().getId(),
                membership.getGroup().getId(),
                membership.getUser().getEmail()
        );
    }
}