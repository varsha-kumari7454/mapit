package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import com.google.gson.Gson;

@Entity
@NamedQueries({
		@NamedQuery(name = "RegisteredApp.findByPrivateAppId", query = "SELECT r FROM RegisteredApp r WHERE r.appSecret=:privateAppId"),
		@NamedQuery(name = "RegisteredApp.findByPublicAppId", query = "SELECT r FROM RegisteredApp r WHERE r.publicAppId=:publicAppId"),
		@NamedQuery(name = "RegisteredApp.findByPublicAppIdOrPrivateAppId", query = "SELECT r FROM RegisteredApp r WHERE r.publicAppId=:publicAppId AND r.appSecret = :privateAppId"),
		@NamedQuery(name = "RegisteredApp.findByName", query = "SELECT r FROM RegisteredApp r WHERE r.name = :name")
})
@Table(name = "RegisteredApp")
public class RegisteredApp {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long UUID;
	private String name;
	private String appSecret;
	private String logoURL;
	private String publicAppId;
	@Type(type = "text")
	private String metaData;
	@ManyToMany(mappedBy = "registeredApps")
	private List<RegisteredAppUser> registeredAppUsers;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}

	public String getLogoURL() {
		return logoURL;
	}

	public void setLogoURL(String logoURL) {
		this.logoURL = logoURL;
	}

	public String getPublicAppId() {
		return publicAppId;
	}

	public void setPublicAppId(String publicAppId) {
		this.publicAppId = publicAppId;
	}

	public Long getUUID() {
		return UUID;
	}

	public void setUUID(Long uUID) {
		this.UUID = uUID;
	}

	public List<RegisteredAppUser> getRegisteredAppUsers() {
		return registeredAppUsers;
	}

	public void setRegisteredAppUsers(List<RegisteredAppUser> registeredAppUsers) {
		this.registeredAppUsers = registeredAppUsers;
	}
	private static final Gson g = new Gson();
	public MetaDataEntityDto getMetaData() {
		return g.fromJson(this.metaData, MetaDataEntityDto.class);
	}

	public void setMetaData(MetaDataEntityDto AppStatus) {
		this.metaData = g.toJson(AppStatus);
	}
	@Override
	public String toString() {
		return "RegisteredApp [UUID=" + UUID + ", name=" + name + ", appSecret=" + appSecret + ", logoURL=" + logoURL
				+ ", publicAppId=" + publicAppId + ", registeredAppUsers=" + registeredAppUsers + "]";
	}
}
