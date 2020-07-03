package dto.acl;

import java.util.List;

public class MinimalACLWebDto {
	
	private Long id;
	private Long ownerId;
	private Long subUserId;
	private List<AccessControlUnitDto> aclUnits;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
	public Long getSubUserId() {
		return subUserId;
	}
	public void setSubUserId(Long subUserId) {
		this.subUserId = subUserId;
	}
	public List<AccessControlUnitDto> getAclUnits() {
		return aclUnits;
	}
	public void setAclUnits(List<AccessControlUnitDto> aclUnits) {
		this.aclUnits = aclUnits;
	}
	
}
