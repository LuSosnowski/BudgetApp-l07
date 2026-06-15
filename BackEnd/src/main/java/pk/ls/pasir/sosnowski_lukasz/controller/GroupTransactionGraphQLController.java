package pk.ls.pasir.sosnowski_lukasz.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.ls.pasir.sosnowski_lukasz.dto.GroupTransactionDTO;
import pk.ls.pasir.sosnowski_lukasz.model.User;
import pk.ls.pasir.sosnowski_lukasz.service.CurrentUserService;
import pk.ls.pasir.sosnowski_lukasz.service.GroupTransactionService;

@Controller
public class GroupTransactionGraphQLController {

    private final GroupTransactionService groupTransactionService;
    private final CurrentUserService currentUserService;

    public GroupTransactionGraphQLController(GroupTransactionService groupTransactionService,
                                             CurrentUserService currentUserService) {
        this.groupTransactionService = groupTransactionService;
        this.currentUserService = currentUserService;
    }

    @MutationMapping
    public Boolean addGroupTransaction(
            @Argument("groupTransactionDTO") @Valid GroupTransactionDTO groupTransactionDTO
    ) {
        User currentUser = currentUserService.getCurrentUser();
        return groupTransactionService.addGroupTransaction(groupTransactionDTO, currentUser);
    }
}