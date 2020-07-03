package dto.acl;

import java.util.ArrayList;
import java.util.List;

import dto.MinimalUserDetailsDto;

public class AccessControlListWebDto {

	private Long ownerId;
	private MinimalUserDetailsDto ownerDetails;
	private List<AccessControlListDto> accessControlList = new ArrayList<>();

	public List<AccessControlListDto> getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(List<AccessControlListDto> accessControlList) {
		this.accessControlList = accessControlList;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public MinimalUserDetailsDto getOwnerDetails() {
		return ownerDetails;
	}

	public void setOwnerDetails(MinimalUserDetailsDto ownerDetails) {
		this.ownerDetails = ownerDetails;
	}
	
}
