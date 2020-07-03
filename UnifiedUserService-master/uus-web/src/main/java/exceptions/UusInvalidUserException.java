package exceptions;

public class UusInvalidUserException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UusInvalidUserException(String string) {
		super(string);
	}
}
