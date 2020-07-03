package facade;

import exceptions.UUSException;
import exceptions.UserNotActiveException;
import models.ValidUserSessionToken;
import dto.TokenDto;
import dto.UserDto;

public interface TokenFacade {

	UserDto createUserToken(String email, Long expiryDuration) throws UserNotActiveException;

	String refreshToken(String token, Long extensionDuration);

	boolean isValidToken(String token);

	boolean isValidUserSessionToken(Long tokenId);

	void removeToken(String token, boolean removeAllToken);

	void removeAllTokenByUuid(Long uuid);

	void removeTokenById(Long tokenId);

	ValidUserSessionToken getValidUserSessionToken(Long tokenId) throws UUSException;

	UserDto getUserDtoByUusToken(String uusToken);

	TokenDto getTokenDtoByToken(String token);

	void flushTokensByUuid(Long uuid);

	void flushTokensByEmailId(String email);

	boolean isHavingAnyUusTokensByUuid(Long uuid);

}
