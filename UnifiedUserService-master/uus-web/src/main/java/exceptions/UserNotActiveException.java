package exceptions;

public class UserNotActiveException extends Exception{
	
	private static final long serialVersionUID = 1L;
	
	public UserNotActiveException(String message) {
		super(message);
	}
}
