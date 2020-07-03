package facade;

import javax.persistence.EntityManager;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import dto.DtoUtils;
import dto.TokenDto;
import dto.UserDto;
import models.UserDetailsEntityDto;
import exceptions.UUSException;
import exceptions.UserNotActiveException;
import models.RegisteredApp;
import models.RegisteredAppUser;
import models.ValidUserSessionToken;
import ninja.utils.NinjaProperties;

public class TokenFacadeImpl implements TokenFacade {
	private static final Logger log = LogManager.getLogger(TokenFacadeImpl.class.getName());

	@Inject
	private Provider<EntityManager> entityManagerProvider;
	@Inject
	NinjaProperties ninjaProperties;
	@Inject
	private UserFacade userFacade;
	@Inject
	private TokenFacade self;

	@Override
	@Transactional
	public UserDto createUserToken(String email, Long expiryDuration) throws UserNotActiveException {
		log.info("createUserToken called email : " + email);
		EntityManager entityManager = entityManagerProvider.get();

		RegisteredAppUser registeredAppUser = userFacade.getRegisteredUserByEmail(email);

		boolean isActive = false;
		if(registeredAppUser.getActive() == null) {
			isActive = true;
		} else {
			isActive = registeredAppUser.getActive();
		}
		System.out.println("isActive : "+isActive);
		if(!isActive) {
			throw new UserNotActiveException("Cannot create Token Since, User is not active.");
		}
		
		boolean hasSuperUser = (registeredAppUser.getSuperUsersCount() > 0);
		boolean createdAsSubUser = registeredAppUser.isCreatedAsSubUser();
		
		TokenDto tokenDto = new TokenDto();
		tokenDto.setAppNameList(DtoUtils.getAppNameAsString(registeredAppUser));
		tokenDto.setEmail(registeredAppUser.getEmail());
		tokenDto.setExpiryDate(System.currentTimeMillis() + expiryDuration);
		tokenDto.setUserRole(registeredAppUser.getRoles().getRoles().get(0).toString());
		tokenDto.setUuid(registeredAppUser.getUuid());
		tokenDto.setCreatedAsSubRole(createdAsSubUser);
		tokenDto.setHasSuperUser(hasSuperUser);
		
		ValidUserSessionToken validUserSessionToken = new ValidUserSessionToken();
		validUserSessionToken.setExpiryDate(tokenDto.getExpiryDate());
		validUserSessionToken.setUuid(tokenDto.getUuid());
		entityManager.persist(validUserSessionToken);

		tokenDto.setTokenId(validUserSessionToken.getId());

		String token = createJWT(tokenDto);
		validUserSessionToken.setToken(token);

		UserDetailsEntityDto userDetailsEntityDto = registeredAppUser.getUserDetails();
		if (userDetailsEntityDto == null) {
			userDetailsEntityDto = new UserDetailsEntityDto();
		}
		String name = userDetailsEntityDto.getName();
		if (name == null) {
			name = email.substring(0, email.indexOf('@'));
		}

		UserDto user = new UserDto();
		user.setRole(registeredAppUser.getRoles().getRoles().get(0).toString());
		user.setToken(token);
		user.setName(name);
		user.setEmail(email);
		user.setActive(isActive);
		user.setSuperUsersCount(registeredAppUser.getSuperUsersCount());
		user.setCreatedAsSubUser(registeredAppUser.isCreatedAsSubUser());
		
		return user;
	}

	private String createJWT(TokenDto tokenDto) {
		String signiture = ninjaProperties.get("uus.token.signiture");
		String token = DtoUtils.getUserToken(tokenDto, signiture);
		return token;
	}

	@Override
	@Transactional
	public String refreshToken(String token, Long extensionDuration) {
		log.info("refreshToken called extensionDuration : " + extensionDuration);
		EntityManager entityManager = entityManagerProvider.get();

		TokenDto tokenDto = extractToken(token);
		ValidUserSessionToken validUserSessionToken = entityManager.find(ValidUserSessionToken.class,
				tokenDto.getTokenId());
		validUserSessionToken.extendExpiryDate(extensionDuration);
		tokenDto.setExpiryDate(validUserSessionToken.getExpiryDate());

		return createJWT(tokenDto);
	}

	@Override
	public boolean isValidToken(String token) {
		log.info("isValidToken called");
		TokenDto tokenDto = extractToken(token);
		return self.isValidUserSessionToken(tokenDto.getTokenId());
	}

	@Override
	@Transactional
	public ValidUserSessionToken getValidUserSessionToken(Long tokenId) throws UUSException {
		EntityManager entityManager = entityManagerProvider.get();
		if (tokenId == null) {
			throw new UUSException("Token Id is undefined");
		}
		ValidUserSessionToken validUserSessionToken = entityManager.find(ValidUserSessionToken.class, tokenId);
		if (validUserSessionToken == null) {
			throw new UUSException("Not a valid session. please login");
		}
		if (validUserSessionToken.getExpiryDate() < System.currentTimeMillis()) {
			throw new UUSException("Expired token");
		}
		return validUserSessionToken;
	}

	@Override
	@Transactional
	public boolean isValidUserSessionToken(Long tokenId) throws UUSException {
		log.info("isValidUserSessionToken called tokenId : " + tokenId);
		EntityManager entityManager = entityManagerProvider.get();
		if (tokenId == null) {
			return false;
		}
		ValidUserSessionToken validUserSessionToken = entityManager.find(ValidUserSessionToken.class, tokenId);
		if (validUserSessionToken == null) {
			return false;
		}
		/*
		 * if (validUserSessionToken.getExpiryDate() <
		 * System.currentTimeMillis()) { return false; }
		 */
		return true;
	}

	private TokenDto extractToken(String token) {
		String signature = ninjaProperties.get("uus.token.signiture");
		return DtoUtils.extractToken(token, signature);
	}

	@Override
	public void removeToken(String token, boolean removeAllToken) {
		log.info("removeToken called removeAllToken : " + removeAllToken);
		TokenDto extractToken = extractToken(token);
		if(isHavingAnyUusTokensByUuid(extractToken.getUuid())) {
			if (removeAllToken) {
				self.removeAllTokenByUuid(extractToken.getUuid());
			}
			self.removeTokenById(extractToken.getTokenId());
		}
	}

	@Override
	@Transactional
	public void removeAllTokenByUuid(Long uuid) {
		log.info("removeAllTokenByUuid called uuid : " + uuid);
		EntityManager entityManager = entityManagerProvider.get();
		entityManager.createQuery("DELETE FROM ValidUserSessionToken v WHERE v.uuid= :uuid").setParameter("uuid", uuid)
				.executeUpdate();
	}

	@Override
	@Transactional
	public void removeTokenById(Long tokenId) {
		log.info("removeTokenById called tokenId : " + tokenId);
		EntityManager entityManager = entityManagerProvider.get();
		entityManager.createQuery("DELETE FROM ValidUserSessionToken v WHERE v.id= :tokenId")
				.setParameter("tokenId", tokenId).executeUpdate();
	}

	@Override
	public UserDto getUserDtoByUusToken(String uusToken) {
		log.info("getUserDtoByUusToken called");
		TokenDto tokenDto = extractToken(uusToken);
		RegisteredAppUser registeredAppUser = userFacade.getRegisteredUserByEmail(tokenDto.getEmail());
		UserDetailsEntityDto userDetailsEntityDto = registeredAppUser.getUserDetails();
		if (userDetailsEntityDto == null) {
			userDetailsEntityDto = new UserDetailsEntityDto();
		}
		String name = userDetailsEntityDto.getName();
		String email = tokenDto.getEmail();
		if (name == null) {
			name = email.substring(0, email.indexOf('@'));
		}
		UserDto user = new UserDto();
		user.setRole(registeredAppUser.getRoles().getRoles().get(0).toString());
		user.setToken(uusToken);
		user.setName(name);
		user.setEmail(email);
		user.setCreatedAsSubUser(registeredAppUser.isCreatedAsSubUser());
		user.setSuperUsersCount(registeredAppUser.getSuperUsersCount());
		boolean isActive = ((registeredAppUser.getActive() == null)?true:registeredAppUser.getActive());
		user.setActive(isActive);
		user.setUuid(registeredAppUser.getUuid());
		
		return user;
	}

	@Override
	public TokenDto getTokenDtoByToken(String token) {
		TokenDto tokenDto = new TokenDto();
		tokenDto = extractToken(token);
		return tokenDto;
	}
	
	@Override
	@Transactional
	public void flushTokensByUuid(Long uuid) {
		log.info("flushTokensByUuid called with uuid : " + uuid);
		EntityManager entityManager = entityManagerProvider.get();
		boolean isHavingTokens = isHavingAnyUusTokensByUuid(uuid);
		log.info("isHavingTokens : "+ isHavingTokens);
		if(isHavingTokens) {			
			entityManager.createNamedQuery("ValidUserSessionToken.flushTokensByUuid")
			.setParameter("uuid", uuid).executeUpdate();
			log.info("Removed all tokens having uuid : "+uuid);
		}
	}
	
	@Override
	@Transactional
	public boolean isHavingAnyUusTokensByUuid(Long uuid) {
		EntityManager entityManager = entityManagerProvider.get();
		boolean isHavingTokens = false;
		
		Long tokensCount = entityManager.createNamedQuery("ValidUserSessionToken.getTokensCountUuid", Long.class)
		.setParameter("uuid", uuid).getSingleResult();
		if(tokensCount > 0) {
			isHavingTokens = true;
		}
		
		return isHavingTokens;
	}
	
	@Override
	public void flushTokensByEmailId(String email) {
		RegisteredAppUser registeredUser = userFacade.getRegisteredUserByEmail(email);
		if(registeredUser != null) {
			flushTokensByUuid(registeredUser.getUuid());
		}
	}
}