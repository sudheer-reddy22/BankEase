
package BankManagement.dto;

import BankManagement.entity.User;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String token;

    public UserResponse(User user) {
        this(user, null);
    }

    public UserResponse(User user, String token) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}