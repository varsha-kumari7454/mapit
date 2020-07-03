package dto.acl;

import dto.MinimalUserDetailsDto;

public class SubUserLogInDto {

	private UusTokenTypeDto tokenType;
	private Long aclId;
	private MinimalUserDetailsDto ownerDetails;
	private SubUserDetailsDto subUserDetails;
	private Long refreshCounter;
	
	public UusTokenTypeDto getTokenType() {
		return tokenType;
	}
	public void setTokenType(UusTokenTypeDto tokenType) {
		this.tokenType = tokenType;
	}
	public Long getAclId() {
		return aclId;
	}
	public void setAclId(Long aclId) {
		this.aclId = aclId;
	}
	public MinimalUserDetailsDto getOwnerDetails() {
		return ownerDetails;
	}
	public void setOwnerDetails(MinimalUserDetailsDto ownerDetails) {
		this.ownerDetails = ownerDetails;
	}
	public SubUserDetailsDto getSubUserDetails() {
		return subUserDetails;
	}
	public void setSubUserDetails(SubUserDetailsDto subUserDetails) {
		this.subUserDetails = subUserDetails;
	}
	public Long getRefreshCounter() {
		return refreshCounter;
	}
	public void setRefreshCounter(Long refreshCounter) {
		this.refreshCounter = refreshCounter;
	}
	
}
