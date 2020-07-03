package dto;

public class MessageDto {
	public String message;

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public String toString() {
		return "MessageDto [message=" + message + "]";
	}
}