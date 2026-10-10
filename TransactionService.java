
package BankManagement.service;

import BankManagement.dto.TransactionRequest;
import BankManagement.entity.Account;
import BankManagement.entity.Transaction;
import BankManagement.repository.AccountRepository;
import BankManagement.repository.TransactionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    private Account getAuthorizedAccount(
            String accountNumber,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please sign in first");
        }

        Long loggedInUserId;

        try {
            loggedInUserId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication");
        }

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        if (!account.getUser().getId().equals(loggedInUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another user's account");
        }

        return account;
    }

    @Transactional
    public Account deposit(
            String accountNumber,
            TransactionRequest request,
            Authentication authentication) {

        Account account = getAuthorizedAccount(
                accountNumber, authentication);

        BigDecimal amount = request.getAmount();

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid positive deposit amount with at most two decimal places");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        saveTransaction(account, "DEPOSIT", amount);

        return account;
    }

    @Transactional
    public Account withdraw(
            String accountNumber,
            TransactionRequest request,
            Authentication authentication) {

        Account account = getAuthorizedAccount(
                accountNumber, authentication);

        BigDecimal amount = request.getAmount();

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid positive withdrawal amount with at most two decimal places");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        saveTransaction(account, "WITHDRAWAL", amount);

        return account;
    }

    @Transactional
    public Account transfer(
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please sign in first");
        }

        Long loggedInUserId;

        try {
            loggedInUserId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authentication");
        }

        if (fromAccountNumber == null || toAccountNumber == null ||
                fromAccountNumber.isBlank() ||
                toAccountNumber.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both account numbers are required");
        }

        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source and destination accounts must be different");
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid positive transfer amount with at most two decimal places");
        }

        Account fromAccount = accountRepository
                .findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Source account not found"));

        Account toAccount = accountRepository
                .findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Destination account not found"));

        if (!fromAccount.getUser().getId().equals(loggedInUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can transfer money only from your own account");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        saveTransaction(fromAccount, "TRANSFER_SENT", amount);
        saveTransaction(toAccount, "TRANSFER_RECEIVED", amount);

        return fromAccount;
    }

    private void saveTransaction(
            Account account, String type, BigDecimal amount) {

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");

        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(
            String accountNumber,
            Authentication authentication) {

        Account account = getAuthorizedAccount(
                accountNumber, authentication);

        return transactionRepository
                .findByAccountOrderByTransactionDateDesc(account);
    }
}