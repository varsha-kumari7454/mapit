package exceptions;

public class UusSuperUserNotFound extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UusSuperUserNotFound(String string) {
		super(string);
	}
}
