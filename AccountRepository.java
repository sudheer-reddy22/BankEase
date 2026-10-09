
package BankManagement.repository;

import BankManagement.entity.Account;
import BankManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUser(User user);

    long countByUser(User user);

    long countByAccountType(String accountType);
}

