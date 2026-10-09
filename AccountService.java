
package BankManagement.service;

import BankManagement.entity.Account;
import BankManagement.entity.User;
import BankManagement.repository.AccountRepository;
import BankManagement.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Account createAccount(Long userId, String accountType) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (accountType == null ||
                !(accountType.equalsIgnoreCase("Savings")
                || accountType.equalsIgnoreCase("Current"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account type must be Savings or Current"
            );
        }

        Account account = new Account();

        account.setAccountNumber(
                UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase()
        );

        account.setAccountType(
                accountType.substring(0, 1).toUpperCase()
                        + accountType.substring(1).toLowerCase()
        );

        account.setBalance(java.math.BigDecimal.ZERO);
        account.setUser(user);

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return accountRepository.findByUser(user);
    }
}