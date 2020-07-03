package exceptions;

public class UusInvalidPublicOrPrivateAppId extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public UusInvalidPublicOrPrivateAppId(String msg){
		super(msg);
	}
}

