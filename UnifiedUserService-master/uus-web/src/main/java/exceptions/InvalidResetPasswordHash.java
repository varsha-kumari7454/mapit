package exceptions;

public class InvalidResetPasswordHash extends Exception {
	private static final long serialVersionUID = 1L;
	
	public InvalidResetPasswordHash(String msg){
		super(msg);
	}
}
