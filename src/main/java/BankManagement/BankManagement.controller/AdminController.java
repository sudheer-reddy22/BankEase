
package BankManagement.controller;

import BankManagement.entity.Account;
import BankManagement.entity.Transaction;
import BankManagement.entity.User;
import BankManagement.repository.AccountRepository;
import BankManagement.repository.TransactionRepository;
import BankManagement.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AdminController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {

        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {

        List<User> users = userRepository.findAll();
        List<Account> accounts = accountRepository.findAll();

        List<Transaction> transactions =
                transactionRepository.findAllByOrderByTransactionDateDesc();

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .filter(balance -> balance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totalCustomers",
                users.stream()
                        .filter(user -> "USER".equals(user.getRole()))
                        .count());

        result.put("totalAdmins",
                users.stream()
                        .filter(user -> "ADMIN".equals(user.getRole()))
                        .count());

        result.put("totalAccounts", accounts.size());
        result.put("totalTransactions", transactions.size());
        result.put("totalBalance", totalBalance);

        return result;
    }

    @GetMapping("/customers")
    public List<Map<String, Object>> getCustomers() {

        return userRepository.findAllByOrderByIdDesc()
                .stream()
                .map(user -> {

                    Map<String, Object> customer =
                            new LinkedHashMap<>();

                    customer.put("id", user.getId());
                    customer.put("name", user.getName());
                    customer.put("email", user.getEmail());
                    customer.put("phone", user.getPhone());
                    customer.put("role", user.getRole());
                    customer.put("status", user.getStatus());

                    customer.put("accountCount",
                            accountRepository.findByUser(user).size());

                    return customer;
                })
                .toList();
    }

    @PutMapping("/customers/{id}/status")
    public Map<String, Object> updateCustomerStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACTIVE or INACTIVE");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"));

        if (!"USER".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only customer accounts can be updated here");
        }

        user.setStatus(status);
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Customer status updated successfully");
        result.put("id", user.getId());
        result.put("status", user.getStatus());

        return result;
    }

    @GetMapping("/accounts")
    public List<Map<String, Object>> getAccounts() {

        return accountRepository.findAll()
                .stream()
                .map(account -> {

                    Map<String, Object> item =
                            new LinkedHashMap<>();

                    item.put("accountNumber",
                            account.getAccountNumber());

                    item.put("accountType",
                            account.getAccountType());

                    item.put("balance",
                            account.getBalance());

                    item.put("status",
                            account.getStatus());

                    item.put("customerName",
                            account.getUser().getName());

                    item.put("customerEmail",
                            account.getUser().getEmail());

                    return item;
                })
                .toList();
    }

    @PutMapping("/accounts/{accountNumber}/status")
    public Map<String, Object> updateAccountStatus(
            @PathVariable String accountNumber,
            @RequestParam String status) {

        if (!status.equals("ACTIVE") && !status.equals("FROZEN")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACTIVE or FROZEN");
        }

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found"));

        account.setStatus(status);
        accountRepository.save(account);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Account status updated successfully");
        result.put("accountNumber", account.getAccountNumber());
        result.put("status", account.getStatus());

        return result;
    }

    @GetMapping("/transactions")
    public List<Map<String, Object>> getTransactions() {

        return transactionRepository
                .findAllByOrderByTransactionDateDesc()
                .stream()
                .map(transaction -> {

                    Map<String, Object> item =
                            new LinkedHashMap<>();

                    item.put("id", transaction.getId());
                    item.put("transactionType",
                            transaction.getTransactionType());

                    item.put("amount", transaction.getAmount());

                    item.put("transactionDate",
                            transaction.getTransactionDate());

                    item.put("status", transaction.getStatus());

                    item.put("accountNumber",
                            transaction.getAccount().getAccountNumber());

                    item.put("customerName",
                            transaction.getAccount()
                                    .getUser().getName());

                    return item;
                })
                .toList();
    }
}
