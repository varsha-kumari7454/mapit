package models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.h2.util.HashBase;

public class AppStatusEntityDto {
	@Enumerated(EnumType.STRING)
	private Servers server;
	
	Map<Servers, AppStatusUnitEntityDto> appStatusUnits = new HashMap<>();
	public Map<Servers, AppStatusUnitEntityDto> getAppStatusUnits() {
		return appStatusUnits;
	}
	public void setAppStatusUnits(Map<Servers, AppStatusUnitEntityDto> appStatusUnits) {
		this.appStatusUnits = appStatusUnits;
	};
}
