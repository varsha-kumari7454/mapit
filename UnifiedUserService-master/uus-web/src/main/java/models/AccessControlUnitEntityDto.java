package models;

import java.util.List;

public class AccessControlUnitEntityDto {
	
	private AccessArea area;
	private List<AccessPermission> permissions;
	private Long accessGrantedFrom;
	private Long accessGrantedTill;
	
	public AccessArea getArea() {
		return area;
	}
	public void setArea(AccessArea area) {
		this.area = area;
	}
	public List<AccessPermission> getPermissions() {
		return permissions;
	}
	public void setPermissions(List<AccessPermission> permissions) {
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
