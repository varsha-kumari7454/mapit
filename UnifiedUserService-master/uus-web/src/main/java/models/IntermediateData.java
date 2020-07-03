package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "IntermediateData")
@NamedQueries({ @NamedQuery(name = "IntermediateData.getAllUsers", query = "SELECT u FROM IntermediateData u"),
		@NamedQuery(name = "IntermediateData.getUserByEmail", query = "SELECT x FROM IntermediateData x where x.mapitEmail= :email OR x.matchEmail= :email") })
public class IntermediateData {

	@Id
	private long Id;

	private String mapitEmail;
	private String mapitPasswordHash;
	private String mapitName;
	private String mapitContactNumber;
	private String mapitRole;
	private Boolean mapitActive;
	private Long mapitRegisteredAt;

	private String matchEmail;
	private String matchPasswordHash;
	private String matchSalt;
	private String matchName;
	private String matchRole;
	private Boolean matchActive;
	private Long matchRegisteredAt;

	private Boolean copiedToRegisteredUser;
	private Boolean passwordSetInRegisteredUser;

	public long getId() {
		return Id;
	}

	public void setId(long id) {
		Id = id;
	}

	public String getMapitEmail() {
		return mapitEmail;
	}

	public void setMapitEmail(String mapitEmail) {
		this.mapitEmail = mapitEmail;
	}

	public String getMapitPasswordHash() {
		return mapitPasswordHash;
	}

	public void setMapitPasswordHash(String mapitPasswordHash) {
		this.mapitPasswordHash = mapitPasswordHash;
	}

	public String getMapitName() {
		return mapitName;
	}

	public void setMapitName(String mapitName) {
		this.mapitName = mapitName;
	}

	public String getMapitContactNumber() {
		return mapitContactNumber;
	}

	public void setMapitContactNumber(String mapitContactNumber) {
		this.mapitContactNumber = mapitContactNumber;
	}

	public String getMapitRole() {
		return mapitRole;
	}

	public void setMapitRole(String mapitRole) {
		this.mapitRole = mapitRole;
	}

	public Boolean isMapitActive() {
		return mapitActive;
	}

	public void setMapitActive(Boolean mapitActive) {
		this.mapitActive = mapitActive;
	}

	public String getMatchEmail() {
		return matchEmail;
	}

	public void setMatchEmail(String matchEmail) {
		this.matchEmail = matchEmail;
	}

	public String getMatchPasswordHash() {
		return matchPasswordHash;
	}

	public void setMatchPasswordHash(String matchPasswordHash) {
		this.matchPasswordHash = matchPasswordHash;
	}

	public String getMatchSalt() {
		return matchSalt;
	}

	public void setMatchSalt(String matchSalt) {
		this.matchSalt = matchSalt;
	}

	public String getMatchName() {
		return matchName;
	}

	public void setMatchName(String matchName) {
		this.matchName = matchName;
	}

	public String getMatchRole() {
		return matchRole;
	}

	public void setMatchRole(String matchRole) {
		this.matchRole = matchRole;
	}

	public Boolean isMatchActive() {
		return matchActive;
	}

	public void setMatchActive(Boolean matchActive) {
		this.matchActive = matchActive;
	}

	public Boolean isCopiedToRegisteredUser() {
		return copiedToRegisteredUser;
	}

	public void setCopiedToRegisteredUser(Boolean copiedToRegisteredUser) {
		this.copiedToRegisteredUser = copiedToRegisteredUser;
	}

	public Boolean isPasswordSetInRegisteredUser() {
		return passwordSetInRegisteredUser;
	}

	public void setPasswordSetInRegisteredUser(Boolean passwordSetInRegisteredUser) {
		this.passwordSetInRegisteredUser = passwordSetInRegisteredUser;
	}

	public Long getMapitRegisteredAt() {
		return mapitRegisteredAt;
	}

	public void setMapitRegisteredAt(Long mapitRegisteredAt) {
		this.mapitRegisteredAt = mapitRegisteredAt;
	}

	public Long getMatchRegisteredAt() {
		return matchRegisteredAt;
	}

	public void setMatchRegisteredAt(Long matchRegisteredAt) {
		this.matchRegisteredAt = matchRegisteredAt;
	}
}
