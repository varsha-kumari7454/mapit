package models;

import java.util.List;

public class IndividualAccessControlListEntityDto {
	List<AccessControlUnitEntityDto> accessControlUnit;

	public List<AccessControlUnitEntityDto> getAccessControlUnit() {
		return accessControlUnit;
	}
	public void setAccessControlUnit(List<AccessControlUnitEntityDto> accessControlUnit) {
		this.accessControlUnit = accessControlUnit;
	}
}
