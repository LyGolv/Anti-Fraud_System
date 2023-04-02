package antifraud.services;

import antifraud.dao.CardLimit;
import antifraud.dao.StolenCard;
import antifraud.dao.SuspiciousIp;
import antifraud.dao.Transaction;
import antifraud.dto.TransactionStatus;
import antifraud.models.Region;
import antifraud.models.TransactionState;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class AntiFraudService {

    final SuspiciousIpRepository ipRepository;
    final StolenCardRepository cardRepository;
    final TransactionRepository transactionRepository;
    final CardLimitRepository cardLimitRepository;

    public AntiFraudService(SuspiciousIpRepository ipRepository, StolenCardRepository cardRepository, TransactionRepository transactionRepository, CardLimitRepository cardLimitRepository) {
        this.ipRepository = ipRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.cardLimitRepository = cardLimitRepository;
    }

    public boolean isSuspiciousIpExist(String ip) {
        return ipRepository.findByIp(ip).isPresent();
    }

    public SuspiciousIp saveSuspiciousIp(SuspiciousIp ip) {
        return ipRepository.saveAndFlush(ip);
    }

    public boolean isValidIP(String ip) {
        return ip.matches("^(25[0-5]|2[0-4]\\d|1?\\d\\d?)(\\.(25[0-5]|2[0-4]\\d|1?\\d\\d?)){3}$");
    }

    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) return false;
        int sumDigit = 0;
        boolean isEven = false;
        for (int i = cardNumber.length()-1; i >= 0 ; --i) {
            int nb = cardNumber.charAt(i) - '0';
            nb = isEven ? nb * 2 : nb;
            sumDigit +=  nb / 10 + nb % 10;
            isEven = !isEven;
        }
        return sumDigit % 10 == 0;
    }

    public List<SuspiciousIp> showSuspiciousIp() {
        return ipRepository.findAll();
    }

    public boolean isStolenCardExist(String number) {
        return cardRepository.findByNumber(number).isPresent();
    }

    public StolenCard saveStolenCard(StolenCard card) {
        return cardRepository.saveAndFlush(card);
    }

    public void deleteStolenCard(String number) {
        cardRepository.deleteById(Objects.requireNonNull(cardRepository.findByNumber(number).orElse(null)).getId());
    }

    public List<StolenCard> showStolenCard() {
        return cardRepository.findAll();
    }

    public void deleteSuspiciousIp(String ip) {
        ipRepository.deleteById(Objects.requireNonNull(ipRepository.findByIp(ip).orElse(null)).getId());
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.saveAndFlush(transaction);
    }

    public boolean isValidRegion(String region) {
        return Arrays.stream(Region.values()).anyMatch(value -> value.name().equalsIgnoreCase(region));
    }

    public boolean isValidDate(LocalDateTime date) {
        if (date == null) return false;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        try {
            formatter.format(date);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    public TransactionStatus getTransactionState(LocalDateTime date) {
        LocalDateTime prevDate = date.minusHours(1);
        List<Transaction> byDateBetween = transactionRepository.findByDateBetween(prevDate, date);
        long countRegion = byDateBetween.stream().map(Transaction::getRegion).distinct().count();
        long countIp = byDateBetween.stream().map(Transaction::getIp).distinct().count();
        if (countRegion == 3) return new TransactionStatus(TransactionState.MANUAL_PROCESSING.value(), "region-correlation");
        else if (countIp == 3)   return new TransactionStatus(TransactionState.MANUAL_PROCESSING.value(), "ip-correlation");
        else if (countRegion > 3) return new TransactionStatus(TransactionState.PROHIBITED.value(), "region-correlation");
        else if (countIp > 3) return new TransactionStatus(TransactionState.PROHIBITED.value(), "ip-correlation");
        return new TransactionStatus("", "");
    }

    public TransactionStatus checkTransactionAmount(Transaction transaction, String result, CardLimit limit) {
        String info = "";
        if (transaction.getAmount() <= limit.getMax_allowed() && result.isEmpty()) {
            result = TransactionState.ALLOWED.value();
            info = "none";
        }
        else if (transaction.getAmount() <= limit.getMax_manual() && result.isEmpty()) {
            result = TransactionState.MANUAL_PROCESSING.value();
            info = "amount";
        }
        else if (transaction.getAmount() > limit.getMax_manual()) {
            result = TransactionState.PROHIBITED.value();
            info = "amount";
        }
        return new TransactionStatus(result, info);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getAllTransactionsByNumber(String number) {
        return transactionRepository.findAllByNumber(number);
    }

    public boolean isTransactionExist(Long transactionId) {
        return transactionRepository.findById(transactionId).isPresent();
    }

    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId).orElse(null);
    }

    public CardLimit saveCardLimit(CardLimit cardLimit) {
        return cardLimitRepository.saveAndFlush(cardLimit);
    }

    public CardLimit getCardLimit(String number) {
        return cardLimitRepository.findByNumber(number).orElse(null);
    }
}
