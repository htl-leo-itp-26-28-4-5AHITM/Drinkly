package at.htl;

import jakarta.persistence.Embeddable;

@Embeddable
public class User {
    public String username;

    public User() {
    }
}