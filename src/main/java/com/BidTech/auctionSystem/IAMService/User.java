package com.BidTech.auctionSystem.IAMService;

import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "Users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String first_name;
	private String last_name;
	private String userName;
	private String email;
	private String password;
	private String role;
	private String streetNumber;
	private String streetName;
	private String city;
	private String postalCode;
	private String country;


	

	public User() {}

	// Constructor
	public User(String first_name, String last_name, String userName,
				String email, String password, String role,String streetNumber, String streetName, String city,
				   String postalCode, String country) {

		this.first_name = first_name;
		this.last_name = last_name;
		this.userName = userName;
		this.email = email;
		this.password = password;
		this.role = role;
		//this.address = address;
		this.streetNumber = streetNumber;
		this.streetName = streetName;
		this.city = city;
		this.postalCode = postalCode;
		this.country = country;
	}

	// getters

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return first_name;
	}

	public String getLastName() {
		return last_name;
	}

	public String getUserName() {
		return userName;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public String getRole() {
		return role;
	}
	public String getStreetNumber() {
		return streetNumber;
	}

	
	public String getStreetName() {
		return streetName;
	}

	

	public String getCity() {
		return city;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCountry() {
		return country;
	}


	

	// setters

	public void setId(Long id) {
		this.id = id;
	}

	public void setFirstName(String first_name) {
		this.first_name = first_name;
	}

	public void setLastName(String last_name) {
		this.last_name = last_name;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setRole(String role) {
		this.role = role;
	}
	public void setStreetNumber(String streetNumber) {
		this.streetNumber = streetNumber;
	}
	public void setCountry(String country) {
		this.country = country;
	}


	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public void setCity(String city) {
		this.city = city;
	}
	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}
	// password check
	public boolean checkPassword(String enteredPassword) {
		return password != null && password.equals(enteredPassword);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, first_name, last_name, role);
	}

	@Override
	public String toString() {
		return "User{" +
				"id=" + id +
				", firstName='" + first_name + '\'' +
				", lastName='" + last_name + '\'' +
				", role='" + role + '\'' +
				", email='" + email + '\'' +
				'}';
	}
}