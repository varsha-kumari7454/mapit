package dto;

public class RegisteredAppDto {
	private Long UUID;
	private String name;
	private String appSecret;
	private String logoURL;
	private String publicAppId;

	public Long getUUID() {
		return UUID;
	}

	public void setUUID(Long uUID) {
		UUID = uUID;
	}

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

	@Override
	public String toString() {
		return "RegisteredAppDto [UUID=" + UUID + ", name=" + name + ", appSecret=" + appSecret + ", logoURL=" + logoURL
				+ ", publicAppId=" + publicAppId + "]";
	}
}
