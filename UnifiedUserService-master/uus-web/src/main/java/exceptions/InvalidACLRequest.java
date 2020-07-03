package exceptions;

public class InvalidACLRequest extends Exception {
	private static final long serialVersionUID = 1L;
	
	public InvalidACLRequest(String msg){
		super(msg);
	}

}
