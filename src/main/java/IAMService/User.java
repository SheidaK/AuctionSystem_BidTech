package IAMService;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
@Entity
public class User {
	private @Id @GeneratedValue Long id;

	//private int id;
	private String first_name;
	private String last_name;
	private String userName;
	private String email;
	private String password;
	 @OneToOne
	   @JoinColumn(name = "address_id")
	    private Address address;
	private String role;
	 public User() {
	    }
	public User(String first_name, String lastName, String userName, String email, String password,String role) {
		this.first_name = first_name;
		this.last_name= lastName;
		this.userName = userName;
		this.email = email;
		this.password = password;
		this.role = role;
	}
	// Getters
	public Long getId() {
		return id;
	}
	public String getRole() {
		return role;
	}
	public String getFirstName() {
		return first_name;
	}
	public String getLastName() {
		return last_name;
	}
	public String getUsername() {
		return userName;
	}
	public String getEmail() {
		return email;
	}
	public Address getAddress() {
		return address;
	}
	public boolean checkPassword(String enteredPassword) {
		return enteredPassword==password;
	}
		// Setters
	public void setId(Long id) {
		this.id = id;
	}
	public void setFirstName(String name) {
		this.first_name = name;
	}
	public void setLastName(String name) {
		this.last_name = name;
	}
	public void setUsername(String username) {
		this.userName = username;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public void setAddress(String streetNumber, String streetName, String city, String postalCode, String country) {
		Address newAddress = new Address(streetNumber,streetName,city, postalCode,country);
		this.address = newAddress;
	}
	public void setRole(String role) {
		this.role = role;
	}
	 @Override
	  public int hashCode() {
	    return Objects.hash(this.id, this.first_name, this.last_name, this.role);
	  }

	  @Override
	  public String toString() {
	    return "User{" + "id=" + this.id + ", firstName='" + this.first_name + '\'' + ", lastName='" + this.last_name
	        + '\'' + ", role='" + this.role + '\'' +", email='" + this.email+ '\''+ '}';
	  }
}
