package pk.ls.pasir.sosnowski_lukasz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.ls.pasir.sosnowski_lukasz.dto.DebtDTO;
import pk.ls.pasir.sosnowski_lukasz.model.Debt;
import pk.ls.pasir.sosnowski_lukasz.model.Group;
import pk.ls.pasir.sosnowski_lukasz.model.Transaction;
import pk.ls.pasir.sosnowski_lukasz.model.TransactionType;
import pk.ls.pasir.sosnowski_lukasz.model.User;
import pk.ls.pasir.sosnowski_lukasz.repository.DebtRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.GroupRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.TransactionRepository;
import pk.ls.pasir.sosnowski_lukasz.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final MembershipService membershipService;

    public DebtService(DebtRepository debtRepository,
                       GroupRepository groupRepository,
                       UserRepository userRepository,
                       TransactionRepository transactionRepository,
                       CurrentUserService currentUserService,
                       MembershipService membershipService) {
        this.debtRepository = debtRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.membershipService = membershipService;
    }

    public List<Debt> getGroupDebts(Long groupId) {
        membershipService.assertCurrentUserIsGroupMember(groupId);
        return debtRepository.findByGroupId(groupId);
    }

    public Debt createDebt(DebtDTO debtDTO) {
        Long groupId = debtDTO.getGroupId();

        membershipService.assertCurrentUserIsGroupMember(groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy o id: " + groupId));

        User debtor = userRepository.findById(debtDTO.getDebtorId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono dłużnika o id: " + debtDTO.getDebtorId()));

        User creditor = userRepository.findById(debtDTO.getCreditorId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wierzyciela o id: " + debtDTO.getCreditorId()));

        if (debtor.getId().equals(creditor.getId())) {
            throw new IllegalStateException("Nie można utworzyć długu do samego siebie");
        }

        membershipService.assertUserIsGroupMember(groupId, debtor.getId());
        membershipService.assertUserIsGroupMember(groupId, creditor.getId());

        User currentUser = currentUserService.getCurrentUser();
        boolean currentUserIsOwner = membershipService.isCurrentUserGroupOwner(groupId);
        boolean currentUserIsParticipant = currentUser.getId().equals(debtor.getId())
                || currentUser.getId().equals(creditor.getId());

        if (!currentUserIsOwner && !currentUserIsParticipant) {
            throw new AccessDeniedException("Członek grupy może utworzyć dług tylko wtedy, gdy jest jego uczestnikiem");
        }

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(debtDTO.getAmount());
        debt.setTitle(debtDTO.getTitle());
        debt.setPaidByDebtor(false);
        debt.setConfirmedByCreditor(false);

        return debtRepository.save(debt);
    }

    public Boolean deleteDebt(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono długu o id: " + debtId));

        assertCurrentUserCanManageDebt(debt);

        debtRepository.delete(debt);

        return true;
    }

    public Debt markDebtAsPaid(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();

        if (!debt.getDebtor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko dłużnik może oznaczyć dług jako opłacony");
        }

        debt.setPaidByDebtor(true);
        debt.setConfirmedByCreditor(false);

        return debtRepository.save(debt);
    }

    public Debt confirmDebtPayment(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();

        if (!debt.getCreditor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko wierzyciel może potwierdzić spłatę długu");
        }

        if (!debt.isPaidByDebtor()) {
            throw new IllegalStateException("Dług musi zostać najpierw oznaczony jako opłacony przez dłużnika");
        }

        debt.setConfirmedByCreditor(true);

        savePaymentTransactions(debt);

        return debtRepository.save(debt);
    }

    private void savePaymentTransactions(Debt debt) {
        Transaction debtorExpense = new Transaction();
        debtorExpense.setAmount(debt.getAmount());
        debtorExpense.setType(TransactionType.EXPENSE);
        debtorExpense.setTags("GROUP_PAYMENT");
        debtorExpense.setNotes("Spłata długu grupowego: " + debt.getTitle());
        debtorExpense.setTimestamp(LocalDateTime.now());
        debtorExpense.setUser(debt.getDebtor());

        Transaction creditorIncome = new Transaction();
        creditorIncome.setAmount(debt.getAmount());
        creditorIncome.setType(TransactionType.INCOME);
        creditorIncome.setTags("GROUP_PAYMENT");
        creditorIncome.setNotes("Potwierdzona spłata długu grupowego: " + debt.getTitle());
        creditorIncome.setTimestamp(LocalDateTime.now());
        creditorIncome.setUser(debt.getCreditor());

        transactionRepository.save(debtorExpense);
        transactionRepository.save(creditorIncome);
    }

    private Debt getDebtForCurrentGroupMember(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono długu o id: " + debtId));

        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());

        return debt;
    }

    public void assertCurrentUserCanManageDebt(Debt debt) {
        Long groupId = debt.getGroup().getId();

        membershipService.assertCurrentUserIsGroupMember(groupId);

        User currentUser = currentUserService.getCurrentUser();

        boolean currentUserIsOwner = membershipService.isCurrentUserGroupOwner(groupId);
        boolean currentUserIsDebtor = debt.getDebtor().getId().equals(currentUser.getId());
        boolean currentUserIsCreditor = debt.getCreditor().getId().equals(currentUser.getId());

        if (!currentUserIsOwner && !currentUserIsDebtor && !currentUserIsCreditor) {
            throw new AccessDeniedException("Nie możesz zarządzać tym długiem");
        }
    }
}