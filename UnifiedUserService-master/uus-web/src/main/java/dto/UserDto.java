package dto;

public class UserDto {
	private String name;
	private String email;
	private String password;
	private String oldPassword;
	private String contactNumber;
	private String role;
	private String expiryDate;
	private String token;
	private String appId;
	private boolean active = false;
	private boolean newUser = false;
	private Long superUsersCount = 0l;
	private boolean createdAsSubUser = false;
	private Long uuid;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getEmail() {
		return email.toLowerCase();
	}

	public void setEmail(String email) {
		this.email = email.toLowerCase();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getContactNumber() {
		return contactNumber;
	}

	public void setContactNumber(String contactNumber) {
		this.contactNumber = contactNumber;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getAppId() {
		return this.appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isNewUser() {
		return this.newUser;
	}

	public void setNewUser(boolean newUser) {
		this.newUser = newUser;
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

	public Long getUuid() {
		return uuid;
	}

	public void setUuid(Long uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "UserDto [name=" + name + ", email=" + email + ", password=" + password + ", contactNumber="
				+ contactNumber + ", role=" + role + ", token=" + token + ", appId" + appId + 
				", superUsersCount :" + superUsersCount + ", createdAsSubUser " + createdAsSubUser +  ", uuid " + uuid + " ]";
	}
}
