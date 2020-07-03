package dto;

public class RejectedUserDto {

	private String email;
	private RejectedReason reason;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public RejectedReason getReason() {
		return reason;
	}

	public void setReason(RejectedReason reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "RejectedUserDto [email=" + email + ", reason=" + reason + "]";
	}
}
