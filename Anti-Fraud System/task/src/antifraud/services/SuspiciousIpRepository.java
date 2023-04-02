package antifraud.services;

import antifraud.dao.SuspiciousIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuspiciousIpRepository extends JpaRepository<SuspiciousIp, Long> {
    Optional<SuspiciousIp> findByIp(String ip);
}
