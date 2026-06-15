package pk.ls.pasir.sosnowski_lukasz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.ls.pasir.sosnowski_lukasz.dto.GroupDTO;
import pk.ls.pasir.sosnowski_lukasz.model.Group;
import pk.ls.pasir.sosnowski_lukasz.model.Membership;
import pk.ls.pasir.sosnowski_lukasz.model.User;
import pk.ls.pasir.sosnowski_lukasz.repository.DebtRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.GroupRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.MembershipRepository;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public GroupService(GroupRepository groupRepository,
                        MembershipRepository membershipRepository,
                        DebtRepository debtRepository,
                        CurrentUserService currentUserService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public List<Group> getMyGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser);
    }

    public Group createGroup(GroupDTO groupDTO) {
        User owner = currentUserService.getCurrentUser();

        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(owner);

        Group savedGroup = groupRepository.save(group);

        Membership membership = new Membership();
        membership.setGroup(savedGroup);
        membership.setUser(owner);

        membershipRepository.save(membership);

        return savedGroup;
    }

    @Transactional
    public Boolean deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy o id: " + id));

        User currentUser = currentUserService.getCurrentUser();

        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może ją usunąć");
        }

        debtRepository.deleteByGroupId(id);
        membershipRepository.deleteByGroupId(id);
        groupRepository.delete(group);

        return true;
    }
}