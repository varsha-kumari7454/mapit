package exceptions;

public class InvalidAccessPermission extends Exception{

	private static final long serialVersionUID = 1L;
	
	public InvalidAccessPermission(String message) {
		super(message);
	}
}
