package dto.acl;

public class SubUserEncryptedDto {

	private String tokenType;
	private String aclId;
	private String ownerDetails;
	private String subUserDetails;
	private String refreshCounter;
	
	public String getTokenType() {
		return tokenType;
	}
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	public String getAclId() {
		return aclId;
	}
	public void setAclId(String aclId) {
		this.aclId = aclId;
	}
	public String getOwnerDetails() {
		return ownerDetails;
	}
	public void setOwnerDetails(String ownerDetails) {
		this.ownerDetails = ownerDetails;
	}
	public String getSubUserDetails() {
		return subUserDetails;
	}
	public void setSubUserDetails(String subUserDetails) {
		this.subUserDetails = subUserDetails;
	}
	public String getRefreshCounter() {
		return refreshCounter;
	}
	public void setRefreshCounter(String refreshCounter) {
		this.refreshCounter = refreshCounter;
	}
	
}
