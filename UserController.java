
package BankManagement.controller;

import BankManagement.dto.LoginRequest;
import BankManagement.dto.UserResponse;
import BankManagement.entity.User;
import BankManagement.service.UserService;
import BankManagement.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(
            UserService userService,
            JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public UserResponse register(@RequestBody User user) {
        User savedUser = userService.registerUser(user);
        return new UserResponse(savedUser);
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request) {

        User user = userService.loginUser(
                request.getEmail(),
                request.getPassword()
        );

        String token = jwtService.generateToken(
        user.getId(),
        user.getEmail(),
        user.getRole()
);

        return new UserResponse(user, token);
    }

    @GetMapping("/profile/{userId}")
    public UserResponse getProfile(
            @PathVariable Long userId,
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

        if (!loggedInUserId.equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another user's profile"
            );
        }

        User user = userService.getUserById(userId);
        return new UserResponse(user);
    }
}