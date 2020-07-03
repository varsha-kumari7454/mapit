package dto;

public class TokenDto {
	private String email;
	private String appNameList;
	private Long uuid;
	private Long expiryDate;
	private String userRole;
	private String token;
	private Long tokenId;
	private boolean createdAsSubRole;
	private boolean hasSuperUser;
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAppNameList() {
		return appNameList;
	}

	public void setAppNameList(String appNameList) {
		this.appNameList = appNameList;
	}

	public Long getUuid() {
		return uuid;
	}

	public void setUuid(Long uuid) {
		this.uuid = uuid;
	}

	public Long getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Long expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getTokenId() {
		return tokenId;
	}

	public void setTokenId(Long tokenId) {
		this.tokenId = tokenId;
	}

	public boolean isCreatedAsSubRole() {
		return createdAsSubRole;
	}

	public void setCreatedAsSubRole(boolean createdAsSubRole) {
		this.createdAsSubRole = createdAsSubRole;
	}

	public boolean isHasSuperUser() {
		return hasSuperUser;
	}

	public void setHasSuperUser(boolean hasSuperUser) {
		this.hasSuperUser = hasSuperUser;
	}

	@Override
	public String toString() {
		return "TokenDto [email=" + email + ", appNameList=" + appNameList + ", uuid=" + uuid + ", expiryDate="
				+ expiryDate + ", userRole=" + userRole + ", token=" + token + 
				", hasSuperUser :" + hasSuperUser + ", createdAsSubRole " + createdAsSubRole + " ]";
	}
}
