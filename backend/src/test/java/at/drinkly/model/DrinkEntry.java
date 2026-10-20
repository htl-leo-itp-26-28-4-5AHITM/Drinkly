package at.drinkly.model;

import java.sql.Timestamp;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class DrinkEntry extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    public Timestamp timestamp;

    @Embedded
    public User user;

    public double drinkingAmount;

    public int getId() {
        return this.id;
    }
}