package com.BidTech.auctionSystem.IAMService;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User — JPA entity representing a registered user of the BidTech platform.
 *
 * <p>Users can have one of two roles:
 * <ul>
 *   <li>{@code BUYER} — can browse the catalogue and place bids</li>
 *   <li>{@code SELLER} — can list products in the catalogue</li>
 * </ul>
 *
 * <p>The full shipping address is stored on the user record and used by
 * {@link com.BidTech.auctionSystem.payment.service.PaymentService} to populate receipts.
 *
 * <p>This entity is persisted in {@code IAM.db} via the {@code iamEntityManagerFactory}
 * configured in {@link com.BidTech.auctionSystem.config.IamDbConfig}.
 *
 * <p><b>Security note:</b> Passwords are stored in plain text. A production system
 * should use BCrypt or another password hashing algorithm.
 */
@Entity
@Table(name = "Users")
public class User {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String first_name;
    private String last_name;

    /** Unique login username used for authentication lookups. */
    private String userName;

    private String email;

    /**
     * Plain-text password — should be hashed in production.
     */
    private String password;

    /** Role in the system: "BUYER" or "SELLER". */
    private String role;

    // Shipping address fields — used by PaymentService to build receipts
    private String streetNumber;
    private String streetName;
    private String city;
    private String postalCode;
    private String country;

    /** Default no-argument constructor required by JPA. */
    public User() {}

    /**
     * Creates a fully populated user.
     *
     * @param first_name   the user's first name
     * @param last_name    the user's last name
     * @param userName     the unique login username
     * @param email        the user's email address
     * @param password     the user's password (plain text)
     * @param role         the user's role ("BUYER" or "SELLER")
     * @param streetNumber the street number of the shipping address
     * @param streetName   the street name of the shipping address
     * @param city         the city of the shipping address
     * @param postalCode   the postal code of the shipping address
     * @param country      the country of the shipping address
     */
    public User(String first_name, String last_name, String userName,
                String email, String password, String role,
                String streetNumber, String streetName, String city,
                String postalCode, String country) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.streetNumber = streetNumber;
        this.streetName = streetName;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()           { return id; }
    public String getFirstName()  { return first_name; }
    public String getLastName()   { return last_name; }
    public String getUserName()   { return userName; }
    public String getEmail()      { return email; }
    public String getPassword()   { return password; }
    public String getRole()       { return role; }
    public String getStreetNumber() { return streetNumber; }
    public String getStreetName() { return streetName; }
    public String getCity()       { return city; }
    public String getPostalCode() { return postalCode; }
    public String getCountry()    { return country; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(Long id)                     { this.id = id; }
    public void setFirstName(String first_name)    { this.first_name = first_name; }
    public void setLastName(String last_name)      { this.last_name = last_name; }
    public void setUserName(String userName)       { this.userName = userName; }
    public void setEmail(String email)             { this.email = email; }
    public void setPassword(String password)       { this.password = password; }
    public void setRole(String role)               { this.role = role; }
    public void setStreetNumber(String streetNumber) { this.streetNumber = streetNumber; }
    public void setStreetName(String streetName)   { this.streetName = streetName; }
    public void setCity(String city)               { this.city = city; }
    public void setPostalCode(String postalCode)   { this.postalCode = postalCode; }
    public void setCountry(String country)         { this.country = country; }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Checks whether the provided password matches this user's stored password.
     *
     * @param enteredPassword the password to check
     * @return {@code true} if the passwords match; {@code false} otherwise
     */
    public boolean checkPassword(String enteredPassword) {
        return password != null && password.equals(enteredPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, first_name, last_name, role);
    }

    @Override
    public String toString() {
        return "User{id=" + id +
                ", firstName='" + first_name + '\'' +
                ", lastName='" + last_name + '\'' +
                ", role='" + role + '\'' +
                ", email='" + email + '\'' + '}';
    }
}
