package models;

public class AppStatusUnitEntityDto {
	public Long initTime;
	public EmailUpdateStatus status;
	public Long modifiedAt;
	public String userId;
	public String message;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Long getInitTime() {
		return initTime;
	}
	public void setInitTime(Long initTime) {
		this.initTime = initTime;
	}
	public EmailUpdateStatus getStatus() {
		return status;
	}
	public void setStatus(EmailUpdateStatus status) {
		this.status = status;
	}
	public Long getModifiedAt() {
		return modifiedAt;
	}
	public void setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
}
