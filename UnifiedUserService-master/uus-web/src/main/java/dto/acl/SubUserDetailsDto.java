package dto.acl;

import java.util.List;

public class SubUserDetailsDto {

	private Long subUserUuid;
	private String subUseremail;
	private String contactNumber;
	private String role;
	private List<AccessControlUnitDto> permissionDetails;
	
	public Long getSubUserUuid() {
		return subUserUuid;
	}
	public void setSubUserUuid(Long subUserUuid) {
		this.subUserUuid = subUserUuid;
	}
	public String getSubUseremail() {
		return subUseremail;
	}
	public void setSubUseremail(String subUseremail) {
		this.subUseremail = subUseremail;
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
	public List<AccessControlUnitDto> getPermissionDetails() {
		return permissionDetails;
	}
	public void setPermissionDetails(List<AccessControlUnitDto> permissionDetails) {
		this.permissionDetails = permissionDetails;
	}
	
}
