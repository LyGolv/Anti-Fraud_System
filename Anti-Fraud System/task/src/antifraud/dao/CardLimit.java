package antifraud.dao;

import javax.persistence.*;

@Entity
@Table(name = "CARDS_LIMIT")
public class CardLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String number;
    private Long max_allowed;
    private Long max_manual;

    public CardLimit(String number, Long max_allowed, Long max_manual) {
        this.number = number;
        this.max_allowed = max_allowed;
        this.max_manual = max_manual;
    }

    public CardLimit() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getMax_allowed() {
        return max_allowed;
    }

    public void setMax_allowed(Long max_allowed) {
        this.max_allowed = max_allowed;
    }

    public Long getMax_manual() {
        return max_manual;
    }

    public void setMax_manual(Long max_manual) {
        this.max_manual = max_manual;
    }
}
