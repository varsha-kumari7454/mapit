package models;

import java.util.ArrayList;
import java.util.List;

public class RolesEntityDto {
	private List<UserRole> roles;

	public List<UserRole> getRoles() {
		return roles;
	}

	public void setRoles(List<UserRole> roles) {
		this.roles = roles;
	}

	public void addRole(UserRole role) {
		if (this.roles == null) {
			this.roles = new ArrayList<>();
		}
		this.roles.add(role);
	}

	public boolean removeRole(String role) {
		int indexOfRole = this.roles.indexOf(role);
		if (indexOfRole != -1) {
			this.roles.remove(indexOfRole);
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "RolesEntityDto [roles=" + roles + "]";
	}
}
