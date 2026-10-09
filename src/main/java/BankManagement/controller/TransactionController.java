
package BankManagement.controller;
import BankManagement.dto.TransferRequest;
import org.springframework.security.core.Authentication;
import java.math.BigDecimal;
import BankManagement.dto.TransactionRequest;
import BankManagement.dto.AccountResponse;
import BankManagement.dto.TransactionResponse;
import BankManagement.entity.Account;
import BankManagement.entity.Transaction;
import BankManagement.service.TransactionService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit/{accountNumber}")
    public AccountResponse deposit(
            @PathVariable String accountNumber,
            @RequestBody TransactionRequest request,
            Authentication authentication) {

        Account account = transactionService.deposit(
                accountNumber, request, authentication);

        return new AccountResponse(account);
    }

    @PostMapping("/withdraw/{accountNumber}")
    public AccountResponse withdraw(
            @PathVariable String accountNumber,
            @RequestBody TransactionRequest request,
            Authentication authentication) {

        Account account = transactionService.withdraw(
                accountNumber, request, authentication);

        return new AccountResponse(account);
    }

    @GetMapping("/history/{accountNumber}")
    public List<TransactionResponse> getTransactionHistory(
            @PathVariable String accountNumber,
            Authentication authentication) {

        List<Transaction> transactions =
                transactionService.getTransactionHistory(
                        accountNumber, authentication);

        return transactions.stream()
                .map(TransactionResponse::new)
                .toList();
    }
    
@PostMapping("/transfer")
public AccountResponse transfer(
        @RequestBody TransferRequest request,
        Authentication authentication) {

    Account account = transactionService.transfer(
            request.getFromAccountNumber(),
            request.getToAccountNumber(),
            request.getAmount(),
            authentication
    );

    return new AccountResponse(account);
}
}