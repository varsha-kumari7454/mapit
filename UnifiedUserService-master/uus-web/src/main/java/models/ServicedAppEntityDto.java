package models;

import java.util.HashSet;
import java.util.Set;

public class ServicedAppEntityDto {
	private Set<String> appIds;

	public Set<String> getAppIds() {
		return appIds;
	}

	public void setAppIds(Set<String> appIds) {
		this.appIds = appIds;
	}

	public void addAppId(String appId) {
		if (this.appIds == null) {
			this.appIds = new HashSet<String>();
		}
		this.appIds.add(appId);
	}

	public boolean removeAppId(String appId) {
		if (this.appIds != null && this.appIds.contains(appId)) {
			this.appIds.remove(appId);
			return true;
		}
		return false;
	}
}