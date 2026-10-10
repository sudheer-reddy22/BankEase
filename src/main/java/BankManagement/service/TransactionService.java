
package BankManagement.service;

import BankManagement.dto.TransactionRequest;
import BankManagement.entity.Account;
import BankManagement.entity.Transaction;
import BankManagement.entity.User;
import BankManagement.repository.AccountRepository;
import BankManagement.repository.TransactionRepository;
import BankManagement.repository.UserRepository;

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
    private final UserRepository userRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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
            loggedInUserId =
                    Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication");
        }

        User loggedInUser = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"));

        if (!"ACTIVE".equalsIgnoreCase(loggedInUser.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your customer account is inactive");
        }

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found"));

        if (!account.getUser().getId().equals(loggedInUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another user's account");
        }

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This bank account is frozen or inactive");
        }

        return account;
    }

    private BigDecimal validateAmount(
            BigDecimal amount, String operation) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid positive " + operation
                            + " amount with at most two decimal places");
        }

        return amount;
    }

    @Transactional
    public Account deposit(
            String accountNumber,
            TransactionRequest request,
            Authentication authentication) {

        Account account = getAuthorizedAccount(
                accountNumber, authentication);

        BigDecimal amount = validateAmount(
                request.getAmount(), "deposit");

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

        BigDecimal amount = validateAmount(
                request.getAmount(), "withdrawal");

        if (account.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient balance");
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

        if (fromAccountNumber == null ||
                toAccountNumber == null ||
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

        amount = validateAmount(amount, "transfer");

        Account fromAccount = getAuthorizedAccount(
                fromAccountNumber, authentication);

        Account toAccount = accountRepository
                .findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Destination account not found"));

        if (!"ACTIVE".equalsIgnoreCase(toAccount.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The destination account is frozen or inactive");
        }

        if (!"ACTIVE".equalsIgnoreCase(
                toAccount.getUser().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The destination customer's account is inactive");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient balance");
        }

        fromAccount.setBalance(
                fromAccount.getBalance().subtract(amount));

        toAccount.setBalance(
                toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        saveTransaction(fromAccount, "TRANSFER_SENT", amount);
        saveTransaction(toAccount, "TRANSFER_RECEIVED", amount);

        return fromAccount;
    }

    private void saveTransaction(
            Account account,
            String type,
            BigDecimal amount) {

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
