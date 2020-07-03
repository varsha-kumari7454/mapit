package dto;

import java.util.List;

public class UusResponseDto {

	public String id;
	public boolean success;
	public List<MessageDto> error;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public List<MessageDto> getError() {
		return error;
	}
	public void setError(List<MessageDto> error) {
		this.error = error;
	}
}
