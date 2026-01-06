package main.java.com.valencmz.fintrack.Model.Entity;

import com.valencmz.fintrack.enums.TransactionType;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Transaction {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "notes", nullable = true)
    private String notes;

    // Relationships
    // category
    // account
    // user

}