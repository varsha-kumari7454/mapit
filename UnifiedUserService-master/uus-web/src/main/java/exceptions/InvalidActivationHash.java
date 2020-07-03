package exceptions;

public class InvalidActivationHash extends Exception{
	private static final long serialVersionUID = 1L;
	
	public InvalidActivationHash(String msg){
		super(msg);
	}
}
