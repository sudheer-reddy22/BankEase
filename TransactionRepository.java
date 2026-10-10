
package BankManagement.repository;

import BankManagement.entity.Account;
import BankManagement.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountOrderByTransactionDateDesc(
            Account account);

    List<Transaction> findAllByOrderByTransactionDateDesc();
}
