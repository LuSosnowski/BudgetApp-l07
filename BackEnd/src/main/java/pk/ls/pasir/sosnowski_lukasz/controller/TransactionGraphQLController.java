package pk.ls.pasir.sosnowski_lukasz.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.ls.pasir.sosnowski_lukasz.dto.BalanceDto;
import pk.ls.pasir.sosnowski_lukasz.dto.TransactionDto;
import pk.ls.pasir.sosnowski_lukasz.model.Transaction;
import pk.ls.pasir.sosnowski_lukasz.model.User;
import pk.ls.pasir.sosnowski_lukasz.service.TransactionService;

import java.util.List;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    public TransactionGraphQLController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.getAllTransactions();
    }

    @QueryMapping
    public BalanceDto userBalance(@Argument Double days) {
        User user = transactionService.getCurrentUser();
        return transactionService.getUserBalance(user, days);
    }

    @MutationMapping
    public Transaction addTransaction(
            @Valid @Argument("transactionDTO") TransactionDto transactionDto
    ) {
        return transactionService.createTransaction(transactionDto);
    }

    @MutationMapping
    public Transaction updateTransaction(
            @Argument Long id,
            @Valid @Argument("transactionDTO") TransactionDto transactionDto
    ) {
        return transactionService.updateTransaction(id, transactionDto);
    }

    @MutationMapping
    public Boolean deleteTransaction(@Argument Long id) {
        transactionService.deleteTransaction(id);
        return true;
    }
}