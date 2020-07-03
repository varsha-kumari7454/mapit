package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostUpdate;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.google.gson.Gson;

@Entity
@Table(name = "RegisteredAppUser")
@NamedQueries({
		@NamedQuery(name = "RegisteredAppUser.findByEmailNhashPassword", query = "SELECT u FROM RegisteredAppUser u WHERE u.email=:email AND hashedPassword = :hashedPassword"),
		@NamedQuery(name = "RegisteredAppUser.findByEmail", query = "SELECT r FROM RegisteredAppUser r WHERE r.email = :email"),
		@NamedQuery(name = "RegisteredAppUser.findCandidateByEmails", query = "SELECT r From RegisteredAppUser r WHERE r.email IN :emails") 
})
public class RegisteredAppUser {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long uuid;
	// Username
	private String email;

	private String hashedPassword;

	@Type(type = "text")
	private String roles;

	@Type(type = "text")
	private String userDetails;

	// TODO relation check
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "RegisteredApp_RegisteredAppUser")
	private List<RegisteredApp> registeredApps;

	private Boolean active;

	private Long registeredAt;

	private Long modifiedAt;

	private boolean userPasswordUpdated = true;
	private String passwordAsString;
	
	private Long superUsersCount = 0l;
	private boolean createdAsSubUser = false;

	// @PostPersist
	protected void onCreate() {
		registeredAt = System.currentTimeMillis();
	}

	@PostUpdate
	protected void onUpdate() {
		modifiedAt = System.currentTimeMillis();
	}

	public Long getRegisteredAt() {
		return registeredAt;
	}

	public void setRegisteredAt(Long registeredAt) {
		this.registeredAt = registeredAt;
	}

	public Long getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public Boolean getActive() {
		return active;
	}

	private static final Gson g = new Gson();

	public Long getUuid() {
		return this.uuid;
	}

	public void setUuid(Long uuid) {
		this.uuid = uuid;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public RolesEntityDto getRoles() {
		return g.fromJson(this.roles, RolesEntityDto.class);
	}

	public void setRoles(RolesEntityDto roles) {
		this.roles = g.toJson(roles);
	}

	public UserDetailsEntityDto getUserDetails() {
		return g.fromJson(this.userDetails, UserDetailsEntityDto.class);
	}

	public void setUserDetails(UserDetailsEntityDto userDetailsEntityDto) {
		this.userDetails = g.toJson(userDetailsEntityDto);
	}

	public String getHashedPassword() {
		return hashedPassword;
	}

	public void setHashedPassword(String hashedPassword) {
		this.hashedPassword = hashedPassword;
	}

	public List<RegisteredApp> getRegisteredApps() {
		return registeredApps;
	}

	public void setRegisteredApps(List<RegisteredApp> registeredApps) {
		this.registeredApps = registeredApps;
	}

	public Boolean isActive() {
		if(active == null) {
			active = true;
		}
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public boolean isUserPasswordUpdate() {
		return this.userPasswordUpdated;
	}

	public void setUserPasswordUpdated(boolean isUserPasswordUpdate) {
		this.userPasswordUpdated = isUserPasswordUpdate;
	}

	public String getPasswordAsString() {
		return this.passwordAsString;
	}

	public void setPasswordAsString(String passwordAsString) {
		this.passwordAsString = passwordAsString;
	}

	public Long getSuperUsersCount() {
		return superUsersCount;
	}

	public void setSuperUsersCount(Long superUsersCount) {
		this.superUsersCount = superUsersCount;
	}

	public boolean isCreatedAsSubUser() {
		return createdAsSubUser;
	}

	public void setCreatedAsSubUser(boolean createdAsSubUser) {
		this.createdAsSubUser = createdAsSubUser;
	}

	@Override
	public String toString() {
		return "[Email : " + email + ", Role : " + roles + ", superUsersCount :" + superUsersCount + ", createdAsSubUser " + createdAsSubUser + " ]";
	}
}
