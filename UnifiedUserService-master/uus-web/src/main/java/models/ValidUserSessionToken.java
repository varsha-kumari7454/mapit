package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "ValidUserSessionToken")
@NamedQueries({
	@NamedQuery(name = "ValidUserSessionToken.flushTokensByUuid", query = "Delete ValidUserSessionToken x where x.uuid = :uuid"),
	@NamedQuery(name = "ValidUserSessionToken.getTokensCountUuid", query = "SELECT COUNT(*) FROM ValidUserSessionToken x where x.uuid = :uuid")
})
public class ValidUserSessionToken {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private Long uuid;
	private Long expiryDate;

	@Type(type = "text")
	private String token;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUuid() {
		return uuid;
	}

	public void setUuid(Long uuid) {
		this.uuid = uuid;
	}

	public Long getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Long expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void extendExpiryDate(Long extensionDuration) {
		this.expiryDate += extensionDuration;
	}
}
