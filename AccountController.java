
package BankManagement.controller;

import BankManagement.dto.AccountResponse;
import BankManagement.entity.Account;
import BankManagement.service.AccountService;
import BankManagement.security.JwtService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin
public class AccountController {

    private final AccountService accountService;
    private final JwtService jwtService;

    public AccountController(
            AccountService accountService,
            JwtService jwtService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    private void verifyUser(Long requestedUserId,
                            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please sign in first"
            );
        }

        Long loggedInUserId;

        try {
            loggedInUserId = Long.parseLong(
                    authentication.getName()
            );
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication"
            );
        }

        if (!loggedInUserId.equals(requestedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another user's accounts"
            );
        }
    }

    @PostMapping("/create/{userId}")
    public AccountResponse createAccount(
            @PathVariable Long userId,
            @RequestParam String accountType,
            Authentication authentication) {

        verifyUser(userId, authentication);

        Account account =
                accountService.createAccount(userId, accountType);

        return new AccountResponse(account);
    }

    @GetMapping("/user/{userId}")
    public List<AccountResponse> getAccountsByUser(
            @PathVariable Long userId,
            Authentication authentication) {

        verifyUser(userId, authentication);

        List<Account> accounts =
                accountService.getAccountsByUser(userId);

        return accounts.stream()
                .map(AccountResponse::new)
                .toList();
    }
}

