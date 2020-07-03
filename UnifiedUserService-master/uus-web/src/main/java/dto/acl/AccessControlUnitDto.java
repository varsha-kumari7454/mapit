package dto.acl;

import java.util.List;

public class AccessControlUnitDto {
	
	private AccessAreaDto area;
	private List<AccessPermissionDto> permissions;
	private Long accessGrantedFrom;
	private Long accessGrantedTill;
	
	public AccessAreaDto getArea() {
		return area;
	}
	public void setArea(AccessAreaDto area) {
		this.area = area;
	}
	public List<AccessPermissionDto> getPermissions() {
		return permissions;
	}
	public void setPermissions(List<AccessPermissionDto> permissions) {
		this.permissions = permissions;
	}
	public Long getAccessGrantedFrom() {
		return accessGrantedFrom;
	}
	public void setAccessGrantedFrom(Long accessGrantedFrom) {
		this.accessGrantedFrom = accessGrantedFrom;
	}
	public Long getAccessGrantedTill() {
		return accessGrantedTill;
	}
	public void setAccessGrantedTill(Long accessGrantedTill) {
		this.accessGrantedTill = accessGrantedTill;
	}
	
}
