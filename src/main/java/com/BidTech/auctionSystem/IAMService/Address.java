package com.BidTech.auctionSystem.IAMService;

import jakarta.persistence.Id;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class Address {

	private String streetNumber;
	private String streetName;
	private String city;
	private String postalCode;
	private String country;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long address_id;

	// REQUIRED by Hibernate
	public Address() {
	}

	public Address(String streetNumber2, String streetName2, String city2, String postalCode2, String country2) {
		this.streetNumber = streetNumber2;
		this.streetName = streetName2;
		this.city = city2;
		this.postalCode = postalCode2;
		this.country = country2;
	}

	public void setStreeNumber(String streetNumber) {
		this.streetNumber = streetNumber;
	}

	public void setStreeName(String streetName) {
		this.streetName = streetName;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getStreeNumber() {
		return streetNumber;
	}

	public String getStreeName() {
		return streetName;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public String getPostalCode() {
		return postalCode;
	}
}