package facade;

public interface AppFacade {

	boolean isValidPublicOrPrivateAppId(String publicAppId, String privateAppId, Boolean noRedirect);

	boolean isValidPublicAppId(String publicAppId);

	void checkRequestedAppValidity(String publicAppId,String privateAppId,Boolean noRedirect);
}
