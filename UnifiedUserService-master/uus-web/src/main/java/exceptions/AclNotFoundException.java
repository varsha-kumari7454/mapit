package exceptions;

public class AclNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AclNotFoundException(String string) {
		super(string);
	}

}
