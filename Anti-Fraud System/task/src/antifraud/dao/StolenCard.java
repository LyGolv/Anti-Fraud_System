package antifraud.dao;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "STOLEN_CARDS")
public class StolenCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String number;

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
}
