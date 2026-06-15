package pk.ls.pasir.sosnowski_lukasz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.ls.pasir.sosnowski_lukasz.dto.GroupExpenseNotification;
import pk.ls.pasir.sosnowski_lukasz.dto.GroupTransactionDTO;
import pk.ls.pasir.sosnowski_lukasz.model.Debt;
import pk.ls.pasir.sosnowski_lukasz.model.Group;
import pk.ls.pasir.sosnowski_lukasz.model.Membership;
import pk.ls.pasir.sosnowski_lukasz.model.Transaction;
import pk.ls.pasir.sosnowski_lukasz.model.TransactionType;
import pk.ls.pasir.sosnowski_lukasz.model.User;
import pk.ls.pasir.sosnowski_lukasz.repository.DebtRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.GroupRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.MembershipRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.TransactionRepository;
import pk.ls.pasir.sosnowski_lukasz.websocket.GroupNotificationWebSocketHandler;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupTransactionService {

    private final DebtRepository debtRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipService membershipService;
    private final TransactionRepository transactionRepository;
    private final GroupNotificationWebSocketHandler groupNotificationWebSocketHandler;

    public GroupTransactionService(DebtRepository debtRepository,
                                   GroupRepository groupRepository,
                                   MembershipRepository membershipRepository,
                                   MembershipService membershipService,
                                   TransactionRepository transactionRepository,
                                   GroupNotificationWebSocketHandler groupNotificationWebSocketHandler) {
        this.debtRepository = debtRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.membershipService = membershipService;
        this.transactionRepository = transactionRepository;
        this.groupNotificationWebSocketHandler = groupNotificationWebSocketHandler;
    }

    public Boolean addGroupTransaction(GroupTransactionDTO groupTransactionDTO, User currentUser) {
        Long groupId = groupTransactionDTO.getGroupId();

        membershipService.assertCurrentUserIsGroupMember(groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy o id: " + groupId));

        List<Membership> members = membershipRepository.findByGroupId(group.getId());

        List<Membership> selectedMembers = selectParticipants(groupTransactionDTO, members, currentUser);

        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma członków, nie można dodać transakcji");
        }

        String type = groupTransactionDTO.getType();

        if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
            throw new IllegalStateException("Typ transakcji musi mieć wartość INCOME albo EXPENSE");
        }

        saveMainTransactionForCurrentUser(groupTransactionDTO, group, currentUser);

        double amountPerUser = groupTransactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(type);

        for (Membership membership : selectedMembers) {
            User otherUser = membership.getUser();

            if (!otherUser.getId().equals(currentUser.getId())) {
                Debt debt = new Debt();

                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(groupTransactionDTO.getTitle());
                debt.setPaidByDebtor(false);
                debt.setConfirmedByCreditor(false);

                debtRepository.save(debt);

                sendGroupExpenseNotification(
                        otherUser,
                        group,
                        groupTransactionDTO,
                        currentUser,
                        amountPerUser
                );
            }
        }

        return true;
    }

    private void saveMainTransactionForCurrentUser(GroupTransactionDTO groupTransactionDTO,
                                                   Group group,
                                                   User currentUser) {
        Transaction transaction = new Transaction();

        transaction.setAmount(groupTransactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(groupTransactionDTO.getType()));
        transaction.setTags("GROUP");
        transaction.setNotes("Transakcja grupowa: " + group.getName() + " - " + groupTransactionDTO.getTitle());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setUser(currentUser);

        transactionRepository.save(transaction);
    }

    private void sendGroupExpenseNotification(User receiver,
                                              Group group,
                                              GroupTransactionDTO groupTransactionDTO,
                                              User currentUser,
                                              double amountPerUser) {
        String message = String.format(
                "%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                currentUser.getEmail(),
                groupTransactionDTO.getTitle(),
                group.getName(),
                amountPerUser
        );

        GroupExpenseNotification notification = new GroupExpenseNotification(
                "GROUP_EXPENSE_ADDED",
                group.getId(),
                group.getName(),
                groupTransactionDTO.getTitle(),
                groupTransactionDTO.getAmount(),
                amountPerUser,
                currentUser.getEmail(),
                message
        );

        groupNotificationWebSocketHandler.sendToUser(receiver.getEmail(), notification);
    }

    private List<Membership> selectParticipants(GroupTransactionDTO groupTransactionDTO,
                                                List<Membership> members,
                                                User currentUser) {
        List<Long> selectedUserIds = groupTransactionDTO.getSelectedUserIds();

        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }

        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);

        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();

        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException("Wszyscy wybrani użytkownicy muszą być członkami grupy");
        }

        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));

        if (!currentUserSelected) {
            throw new IllegalStateException("Aktualny użytkownik musi być uczestnikiem transakcji grupowej");
        }

        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwóch uczestników");
        }

        return selectedMembers;
    }
}