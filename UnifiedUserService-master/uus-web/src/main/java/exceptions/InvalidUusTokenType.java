package exceptions;

public class InvalidUusTokenType extends Exception{

	private static final long serialVersionUID = 1L;
	
	public InvalidUusTokenType(String message) {
		super(message);
	}
}
