package antifraud.controllers;

import antifraud.dao.CardLimit;
import antifraud.dao.StolenCard;
import antifraud.dao.SuspiciousIp;
import antifraud.dao.Transaction;
import antifraud.dto.FeedbackDTO;
import antifraud.dto.RecordStatus;
import antifraud.dto.TransactionStatus;
import antifraud.models.TransactionState;
import antifraud.services.AntiFraudService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/antifraud")
public class AntiFraudController {

    final AntiFraudService service;

    public AntiFraudController(AntiFraudService service) {
        this.service = service;
    }

    @PostMapping("/transaction")
    public ResponseEntity<TransactionStatus> amount(@Valid @RequestBody Transaction transaction) {
        if (!(service.isValidIP(transaction.getIp()) || service.isValidCardNumber(transaction.getNumber()))
                || !service.isValidRegion(transaction.getRegion()) || !service.isValidDate(transaction.getDate()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        if (transaction.getAmount() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransactionStatus(TransactionState.NOT_ALLOWED.value(), "amount"));
        }
        transaction.setFeedback("");
        transaction = service.saveTransaction(transaction);
        CardLimit limit = service.getCardLimit(transaction.getNumber());
        if (limit == null) limit = service.saveCardLimit(new CardLimit(transaction.getNumber(), 200L, 1500L));
        TransactionStatus status = service.getTransactionState(transaction.getDate());
        List<String> info = new ArrayList<>();
        String result = "";
        if (!status.result().isEmpty()) {
            info.add(status.info());
            result = status.result();
        }
        if (service.isStolenCardExist(transaction.getNumber()) && !result.equals(TransactionState.MANUAL_PROCESSING.value())) {
            result = result.isEmpty() ? TransactionState.PROHIBITED.value() : result;
            info.add("card-number");
        }
        if (service.isSuspiciousIpExist(transaction.getIp()) && !result.equals(TransactionState.MANUAL_PROCESSING.value())) {
            result = result.isEmpty() ? TransactionState.PROHIBITED.value() : result;
            info.add("ip");
        }
        TransactionStatus amountStatus = service.checkTransactionAmount(transaction, result, limit);
        result = amountStatus.result().isEmpty() ? result : amountStatus.result();
        if (!amountStatus.info().isEmpty()) info.add(amountStatus.info());
        transaction.setResult(result);
        service.saveTransaction(transaction);
        return ResponseEntity.ok(new TransactionStatus(result, String.join(", ", info.stream().sorted().toList())));
    }

    @PutMapping("/transaction")
    public ResponseEntity<Transaction> updateTransaction(@RequestBody FeedbackDTO feedback) {
        if (!TransactionState.isTransactionState(feedback.feedback()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        else if (!service.isTransactionExist(feedback.transactionId()))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Transaction transaction = service.getTransactionById(feedback.transactionId());
        if (!transaction.getFeedback().isEmpty())
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        else if (transaction.getResult().equalsIgnoreCase(feedback.feedback()))
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        CardLimit limit = service.getCardLimit(transaction.getNumber());
        long increase_allow = (long)Math.ceil(0.8 * limit.getMax_allowed() + 0.2 * transaction.getAmount());
        long decrease_allow = (long)Math.ceil(0.8 * limit.getMax_allowed() - 0.2 * transaction.getAmount());
        long increase_manual = (long)Math.ceil(0.8 * limit.getMax_manual() + 0.2 * transaction.getAmount());
        long decrease_manual = (long)Math.ceil(0.8 * limit.getMax_manual() - 0.2 * transaction.getAmount());
        if (feedback.feedback().equalsIgnoreCase(TransactionState.ALLOWED.value())
                && transaction.getResult().equalsIgnoreCase(TransactionState.MANUAL_PROCESSING.value()))
            limit.setMax_allowed(increase_allow);
        else if (feedback.feedback().equalsIgnoreCase(TransactionState.ALLOWED.value())
                && transaction.getResult().equalsIgnoreCase(TransactionState.PROHIBITED.value())) {
            limit.setMax_allowed(increase_allow);
            limit.setMax_manual(increase_manual);
        }
        else if (feedback.feedback().equalsIgnoreCase(TransactionState.MANUAL_PROCESSING.value())
        && transaction.getResult().equalsIgnoreCase(TransactionState.ALLOWED.value()))
            limit.setMax_allowed(decrease_allow);
        else if (feedback.feedback().equalsIgnoreCase(TransactionState.MANUAL_PROCESSING.value())
        && transaction.getResult().equalsIgnoreCase(TransactionState.PROHIBITED.value()))
            limit.setMax_manual(increase_manual);
        else if (feedback.feedback().equalsIgnoreCase(TransactionState.PROHIBITED.value())
        && transaction.getResult().equalsIgnoreCase(TransactionState.ALLOWED.value())) {
            limit.setMax_allowed(decrease_allow);
            limit.setMax_manual(decrease_manual);
        }
        else if (feedback.feedback().equalsIgnoreCase(TransactionState.PROHIBITED.value())
        && transaction.getResult().equalsIgnoreCase(TransactionState.MANUAL_PROCESSING.value()))
            limit.setMax_manual(decrease_manual);
        service.saveCardLimit(limit);
        transaction.setFeedback(feedback.feedback());
        return ResponseEntity.ok(service.saveTransaction(transaction));
    }

    @GetMapping("/history")
    public List<Transaction> showAllTransactions() {
        return service.getAllTransactions();
    }

    @GetMapping("/history/{number}")
    public ResponseEntity<List<Transaction>> showTransactionByCardNumber(@PathVariable String number) {
        if (!service.isValidCardNumber(number))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        List<Transaction> allTransactionsByNumber = service.getAllTransactionsByNumber(number);
        if (allTransactionsByNumber.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(allTransactionsByNumber);
    }

    @PostMapping("/suspicious-ip")
    public ResponseEntity<SuspiciousIp> addSuspiciousIp(@RequestBody SuspiciousIp ip) {
        if (!service.isValidIP(ip.getIp()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        else if (service.isSuspiciousIpExist(ip.getIp()))
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        return ResponseEntity.ok(service.saveSuspiciousIp(ip));
    }

    @DeleteMapping("/suspicious-ip/{ip}")
    public ResponseEntity<?> deleteSuspiciousIp(@PathVariable String ip) {
        if (!service.isValidIP(ip))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        else if (!service.isSuspiciousIpExist(ip))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        service.deleteSuspiciousIp(ip);
        return ResponseEntity.ok(new RecordStatus("IP " + ip + " successfully removed!"));
    }

    @GetMapping("/suspicious-ip")
    public List<SuspiciousIp> showSuspiciousIp() {
        return service.showSuspiciousIp();
    }

    @PostMapping("/stolencard")
    public ResponseEntity<?> addStolenCard(@RequestBody StolenCard card) {
        if (!service.isValidCardNumber(card.getNumber()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        else if (service.isStolenCardExist(card.getNumber()))
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        return ResponseEntity.ok(service.saveStolenCard(card));
    }

    @DeleteMapping("/stolencard/{number}")
    public ResponseEntity<?> deleteStolenCard(@PathVariable String number) {
        if (!service.isValidCardNumber(number))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        else if (!service.isStolenCardExist(number))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        service.deleteStolenCard(number);
        return ResponseEntity.ok(new RecordStatus("Card " + number + " successfully removed!"));
    }

    @GetMapping("/stolencard")
    public List<StolenCard> showStolenCard() {
        return service.showStolenCard();
    }
}
