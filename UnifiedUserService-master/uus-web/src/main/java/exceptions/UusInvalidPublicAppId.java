package exceptions;

public class UusInvalidPublicAppId extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public UusInvalidPublicAppId(String msg){
		super(msg);
	}
}
