package antifraud.services;

import antifraud.dao.CardLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardLimitRepository extends JpaRepository<CardLimit, Long> {
    Optional<CardLimit> findByNumber(String number);
}
