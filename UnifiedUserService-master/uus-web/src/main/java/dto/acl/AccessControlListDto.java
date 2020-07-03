package dto.acl;

import java.util.List;

import dto.MinimalUserDetailsDto;

public class AccessControlListDto {
	
	private Long id;
	private Long subUserId;
	private MinimalUserDetailsDto subUserDetails;
	private Long creationTime;
	private Long lastmodifiedTime;
	private List<AccessControlUnitDto> aclUnits;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getSubUserId() {
		return subUserId;
	}
	public void setSubUserId(Long subUserId) {
		this.subUserId = subUserId;
	}
	public Long getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Long creationTime) {
		this.creationTime = creationTime;
	}
	public Long getLastmodifiedTime() {
		return lastmodifiedTime;
	}
	public void setLastmodifiedTime(Long lastmodifiedTime) {
		this.lastmodifiedTime = lastmodifiedTime;
	}
	public List<AccessControlUnitDto> getAclUnits() {
		return aclUnits;
	}
	public void setAclUnits(List<AccessControlUnitDto> aclUnits) {
		this.aclUnits = aclUnits;
	}
	public MinimalUserDetailsDto getSubUserDetails() {
		return subUserDetails;
	}
	public void setSubUserDetails(MinimalUserDetailsDto subUserDetails) {
		this.subUserDetails = subUserDetails;
	}
	@Override
	public String toString() {
		return "AccessControlListDto [id=" + id + "subUserId=" + subUserId + 
				"accessGrantedFrom=" + creationTime + "lastModifiedTime=" + lastmodifiedTime + 
				"aclUnits=" + aclUnits + "]";
	}
}