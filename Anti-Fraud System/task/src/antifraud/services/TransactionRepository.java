package antifraud.services;

import antifraud.dao.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDateBetween(LocalDateTime date1, LocalDateTime date2);

    List<Transaction> findAllByNumber(String number);
}
