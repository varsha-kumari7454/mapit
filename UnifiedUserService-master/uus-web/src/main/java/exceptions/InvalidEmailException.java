package exceptions;

public class InvalidEmailException extends RuntimeException{
	private static final long serialVersionUID = 1L;

	public InvalidEmailException(String string) {
		super(string);
	}
}
