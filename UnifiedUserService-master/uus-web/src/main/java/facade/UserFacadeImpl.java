package facade;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.security.acl.AclNotFoundException;

import javax.persistence.EntityManager;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import dto.BulkRegisterResponseDto;
import dto.DtoUtils;
import dto.MessageDto;
import dto.MinimalUserDetailsDto;
import dto.RegisteredAppDto;
import dto.RejectedReason;
import dto.RejectedUserDto;
import dto.TokenDto;
import dto.UserDto;
import dto.UusResponseDto;
import dto.acl.AccessAreaDto;
import dto.acl.AccessControlListDto;
import dto.acl.AccessControlUnitDto;
import dto.acl.AccessPermissionDto;
import dto.acl.MinimalACLWebDto;
import dto.acl.SubUserDetailsDto;
import dto.acl.SubUserEncryptedDto;
import dto.acl.SubUserLogInDto;
import dto.acl.UusTokenTypeDto;
import exceptions.CandidateAlreadyExist;
import exceptions.InvalidAccessArea;
import exceptions.InvalidAccessPermission;
import exceptions.InvalidActivationHash;
import exceptions.InvalidEmailException;
import exceptions.InvalidEmailFormatException;
import exceptions.InvalidResetPasswordHash;
import exceptions.InvalidUusTokenType;
import exceptions.UUSEncryptionException;
import exceptions.UUSException;
import exceptions.UusInvalidUserException;
import models.AccessControlList;
import models.AppStatusEntityDto;
import models.AppStatusUnitEntityDto;
import models.EmailChangeTracker;
import models.EmailUpdateStatus;
import models.IntermediateData;
import models.MetaDataEntityDto;
import models.RegisteredApp;
import models.RegisteredAppUser;
import models.RolesEntityDto;
import models.Servers;
import models.ServicedAppEntityDto;
import models.UserDetailsEntityDto;
import models.UserRole;
import ninja.postoffice.Mail;
import ninja.utils.NinjaProperties;
import services.email.EmailService;

public class UserFacadeImpl implements UserFacade {
	private static final Logger log = LogManager.getLogger(UserFacadeImpl.class.getName());

	@Inject
	private Provider<EntityManager> entityManagerProvider;
	@Inject
	NinjaProperties ninjaProperties;
	@Inject
	private EmailService emailService;
	@Inject
	private UserFacade self;
	@Inject
	private TokenFacade tokenFacade;
	@Inject
	private ACLFacade aclFacade;
	@Inject
	private Provider<Mail> mailProvider;
	@Inject
	private CloseableHttpClient httpClient;
	
	private static final Gson g = new Gson();

	@Override
	@Transactional
	public String registerUser(UserDto user, String publicAppId, String redirectLink, String privateAppId,Boolean noMail, Boolean isReqByAdmin) throws UUSEncryptionException, UUSException {
		log.info("registerUser called");
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredAppUser registeredUser = new RegisteredAppUser();
		registeredUser.setHashedPassword(DtoUtils.getPasswordHash(user.getEmail(), user.getPassword()));
		registeredUser.setPasswordAsString(user.getPassword());
		registeredUser.setEmail(user.getEmail().toLowerCase());
		if(isReqByAdmin!=null && isReqByAdmin) {
			registeredUser.setActive(true);
		}else {
			registeredUser.setActive(false);
		}
		RolesEntityDto rolesEntityDto = new RolesEntityDto();
		rolesEntityDto.addRole(UserRole.valueOf(user.getRole()));
		registeredUser.setRoles(rolesEntityDto);

		ServicedAppEntityDto servicedAppEntityDto = new ServicedAppEntityDto();
		servicedAppEntityDto.addAppId(publicAppId);

		List<RegisteredApp> registeredApps = entityManager
				.createNamedQuery("RegisteredApp.findByPublicAppIdOrPrivateAppId", RegisteredApp.class)
				.setParameter("publicAppId", publicAppId).setParameter("privateAppId", privateAppId).getResultList();
		RegisteredAppDto registeredAppDto = DtoUtils.mapToDto(registeredApps.get(0));

		registeredUser.setRegisteredApps(registeredApps);

		UserDetailsEntityDto userDetailsEntityDto = new UserDetailsEntityDto();
		{
			userDetailsEntityDto.setName(user.getName());
			userDetailsEntityDto.setContactNumber(user.getContactNumber());
		}
		registeredUser.setUserDetails(userDetailsEntityDto);
		entityManager.persist(registeredUser);
		Map<String, String> scope = new HashMap<>();

		String activationCodeHash = DtoUtils.getUserActivationHash(user.getEmail(), registeredUser.getUuid(),
				publicAppId);
		if (noMail != null && noMail) {
			log.info("No mail it noMail : " + noMail);
		} else {
			String activationLink = getActivationLink(user.getEmail(), registeredAppDto.getAppSecret(),
					registeredUser.getUuid(), registeredAppDto, redirectLink, activationCodeHash);
			scope.put("activationLink", activationLink);
			log.info(activationLink);
			String htmlMail = DtoUtils.generateStringFromTemplate("views/email/newUserRegistration.html", scope);
			String txtMail = "";// DtoUtils.generateStringFromTemplate("views/email/newUserVerification.txt",
								// scope);
			String fromEmail = "support@myanatomy.in";
			String toEmail = user.getEmail();
			String subject = "Welcome to " + registeredAppDto.getName();
			mailVerificationLink(toEmail, fromEmail, subject, htmlMail, txtMail, registeredAppDto, activationLink);
		}
		return activationCodeHash;
	}

	@Override
	@Transactional
	public void adminRegisteringUser(UserDto user, String redirectLink, String privateAppId, Boolean sendMail)
			throws UUSEncryptionException, UUSException {
		log.info("adminRegisteringUser called");
		List<RegisteredApp> registeredApps = self.getRegisteredAppByPrivateAppId(privateAppId);
		RegisteredAppDto registeredAppDto = DtoUtils.mapToDto(registeredApps.get(0));
		boolean createdAsSubUser = false;
		RegisteredAppUser registeredUser = self.createUserWithoutPassword(user, registeredApps, createdAsSubUser);
		if (sendMail == null || sendMail == true) {
			String resetPasswordLink = getResetPasswordActivationLink(user.getEmail(), registeredAppDto.getAppSecret(),
					registeredUser.getUuid(), registeredAppDto, redirectLink, registeredUser.getHashedPassword());
			Map<String, String> scope = new HashMap<>();
			scope.put("activationLink", resetPasswordLink);
			scope.put("username", user.getName());
			log.info(resetPasswordLink);
			String htmlMail = DtoUtils.generateStringFromTemplate("views/email/newUserRegistration.html", scope);
			String txtMail = "";
			String fromEmail = "support@myanatomy.in";
			String toEmail = user.getEmail();
			String subject = "Welcome to " + registeredAppDto.getName();
			mailVerificationLink(toEmail, fromEmail, subject, htmlMail, txtMail, registeredAppDto, resetPasswordLink);
		}
	}

	@Override
	@Transactional
	public List<RegisteredApp> getRegisteredAppByPrivateAppId(String privateAppId) {
		log.info("getRegisteredAppByPrivateAppId called");
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredApp> registeredApps = entityManager
				.createNamedQuery("RegisteredApp.findByPrivateAppId", RegisteredApp.class)
				.setParameter("privateAppId", privateAppId).getResultList();
		return registeredApps;
	}

	@Override
	@Transactional
	public RegisteredAppUser createUserWithoutPassword(UserDto user, List<RegisteredApp> registeredApps, boolean createdAsSubUser)
			throws UUSEncryptionException {
		log.info("createUserWithoutPassword called");
		EntityManager entityManager = entityManagerProvider.get();
		String randomString = DtoUtils.randomString(6);
		Long superUsersCount = 0l;

		RegisteredAppUser registeredUser = new RegisteredAppUser();
		registeredUser.setEmail(user.getEmail().toLowerCase());
		registeredUser.setHashedPassword(DtoUtils.getPasswordHash(user.getEmail(), randomString));
		registeredUser.setPasswordAsString(randomString);
		registeredUser.setActive(true);
		
		RolesEntityDto rolesEntityDto = new RolesEntityDto();
		{
			rolesEntityDto.addRole(UserRole.valueOf(user.getRole()));
		}
		registeredUser.setRoles(rolesEntityDto);

		registeredUser.setRegisteredApps(registeredApps);

		UserDetailsEntityDto userDetailsEntityDto = new UserDetailsEntityDto();
		{
			userDetailsEntityDto.setName(user.getName());
			userDetailsEntityDto.setContactNumber(user.getContactNumber());
		}
		registeredUser.setUserDetails(userDetailsEntityDto);
		
		registeredUser.setCreatedAsSubUser(createdAsSubUser);
		registeredUser.setSuperUsersCount(superUsersCount);
		
		entityManager.persist(registeredUser);

		//registeredApps.get(0).getRegisteredAppUsers().add(registeredUser);
		
		return registeredUser;
	}

	private String getActivationLink(String email, String appSecret, Long uuid, RegisteredAppDto registeredAppDto, String redirectLink, String activationCodeHash) throws UUSException {
		String domain = ninjaProperties.get("uus.main");
		String activationLinkTemplate = ninjaProperties.get("uus.activationLink");

		String publicAppId = registeredAppDto.getPublicAppId();
		String activationLink = domain + activationLinkTemplate;

		activationLink = activationLink.replaceAll(":activationCodeHash", activationCodeHash)
				.replaceAll(":email", email).replaceAll(":publicAppId", publicAppId)
				.replaceAll(":redirectLink", redirectLink).replaceAll(":noRedirect", "false");
		return activationLink;
	}

	public String getResetPasswordActivationLink(String email, String appSecret, Long uuid, RegisteredAppDto registeredAppDto, String redirectLink, String password) throws UUSException {
		String domain = ninjaProperties.get("uus.domain");
		String resetPasswordLinkTemplate = ninjaProperties.get("uus.resetPasswordLink");

		String publicAppId = registeredAppDto.getPublicAppId();
		try {
			String resetPasswordCodeHash = DtoUtils.getPasswordResetHash(email, publicAppId, uuid, password);
			String resetPasswordLink = domain + resetPasswordLinkTemplate;

			resetPasswordLink = resetPasswordLink.replaceAll(":resetPasswordCodeHash", resetPasswordCodeHash)
					.replaceAll(":email", email).replaceAll(":publicAppId", publicAppId)
					.replaceAll(":redirectLink", redirectLink);
			return resetPasswordLink;
		} catch (UUSEncryptionException e) {
			throw new UUSException("Could not generate reset password link");
		}
	}

	private void mailVerificationLink(String toEmail, String fromEmail, String subject, String htmlMail, String txtMail,
			RegisteredAppDto registeredAppDto, String activationLink) {
		Mail emailToSend = mailProvider.get();

		// fill the mail with content:
		emailToSend.setSubject(subject);
		emailToSend.setFrom(fromEmail);
		emailToSend.addReplyTo(fromEmail);
		emailToSend.setCharset("utf-8");
		emailToSend.addTo(toEmail);
		emailToSend.setBodyHtml(htmlMail);
		emailToSend.setBodyText(txtMail);
		try {
			emailService.sendAsyncMail(emailToSend);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@Transactional
	public void verifyUser(UserDto user) throws UUSEncryptionException {
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredAppUser registeredAppUser = getUserByEmailNPassword(entityManager, user.getEmail(),
				user.getPassword());
		if (registeredAppUser != null) {
			return;
		}
		throw new UusInvalidUserException("Invalid username or password");
	}

	private RegisteredAppUser getUserByEmailNPassword(EntityManager entityManager, String email, String password)
			throws UUSEncryptionException {
		String hashedPassword = DtoUtils.getPasswordHash(email, password);

		List<RegisteredAppUser> registeredAppUserList = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();

		RegisteredAppUser registeredAppUser = null;
		if (registeredAppUserList == null || registeredAppUserList.size() != 1) {
			log.info("User does not exist : email : " + email);
		} else {
			registeredAppUser = registeredAppUserList.get(0);
			if (!registeredAppUser.isUserPasswordUpdate()) {
				if (self.getIntermediateUser(email, password)) {
					registeredAppUser.setHashedPassword(hashedPassword);
					registeredAppUser.setUserPasswordUpdated(true);
					registeredAppUser.setPasswordAsString(password);
					return registeredAppUser;
				}
				return null;
			}
			// Temporaray Check
			if (registeredAppUser.getHashedPassword() == null || !registeredAppUser.getHashedPassword().equals(hashedPassword)) {
				return null;
			}
		}
		return registeredAppUser;
	}

	@Override
	@Transactional
	public void isValidUserActivationHash(String activationCodeHash, String publicAppId, String email)
			throws UUSEncryptionException, InvalidActivationHash {
		log.info("isValidUserActivationHash called activationCodeHash : " + activationCodeHash + " || publicAppId : "
				+ publicAppId + " || email : " + email);

		RegisteredAppUser registeredAppUser = self.getRegisteredUserByEmail(email);
		Long uuid = registeredAppUser.getUuid();
		String generatedUserActivationHash = DtoUtils.getUserActivationHash(email, uuid, publicAppId);
		if (!activationCodeHash.equals(generatedUserActivationHash)) {
			throw new InvalidActivationHash("Invalid activation link");
		}
	}

	@Override
	@Transactional
	public void updateUserActivationStatus(String email, boolean isActiveUser) {
		log.info("updateActiveUserStatus called : email : " + email + " || " + isActiveUser);
		RegisteredAppUser registeredUser = self.getRegisteredUserByEmail(email);
		registeredUser.setActive(isActiveUser);
	}

	@Override
	@Transactional
	public RegisteredAppUser getRegisteredUserByEmail(String email) {
		log.info("getRegisteredUserByEmail called : " + email);
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredAppUser> registeredUser = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();

		if (registeredUser.size() < 1) {
			throw new UUSException(email + " : user does not exist.");
		}
		if (registeredUser.size() > 1) {
			throw new UUSException(email + " : user has multiple account");
		}
		
		return registeredUser.get(0);
	}

	@Override
	@Transactional
	public long setEmailStatus(String email, String newEmail, boolean b, EmailUpdateStatus emailUpdateStatus, String comment) {
		log.info("setEmailStatus called : " + email);
		EntityManager entityManager = entityManagerProvider.get();
		try {
			EmailChangeTracker newStatus = new EmailChangeTracker();
			newStatus.setFromEmail(email.toLowerCase());
			newStatus.setToEmail(newEmail.toLowerCase());
			newStatus.setEmailChecked(b);
			newStatus.setStatus(emailUpdateStatus);
			newStatus.setComment(comment);
			entityManager.persist(newStatus);
			Long id = newStatus.getId();
			return id;
		} catch (UUSException e) {
			throw new UUSException(email + " : can not change email");
		}
	}

	@Override
	@Transactional
	public String getMapitEmailUpdateUrl(String email, String newEmail) {
		log.info("getMapitEmailUpdateUrl called" + email + " to " + newEmail);
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredApp MapitApp = entityManager.createQuery("SELECT t FROM RegisteredApp t WHERE name=:name", RegisteredApp.class)
												.setParameter("name", "MAPIT")
												.getSingleResult();

		MetaDataEntityDto metaDataEntityDto = MapitApp.getMetaData();
		String url = metaDataEntityDto.getEmailUpdateUrl();
		String appId = MapitApp.getAppSecret();
		url = url.replace("[email]", email);
		url = url.replace("[newEmail]", newEmail);
		url = url.replace("[appId]", appId);
		return url;
	}

	@Override
	@Transactional
	public String getMatchEmailUpdateUrl(String email, String newEmail) {
		log.info("getMatchEmailUpdateUrl called.."+email+"||"+newEmail);
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredApp MatchApp = entityManager.createQuery("SELECT t FROM RegisteredApp t WHERE name=:name", RegisteredApp.class)
										.setParameter("name", "match")
										.getSingleResult();

		MetaDataEntityDto metaDataEntityDto = MatchApp.getMetaData();
		String url = metaDataEntityDto.getEmailUpdateUrl();
		String appId = MatchApp.getAppSecret();
		url = url.replace("[email]", email);
		url = url.replace("[newEmail]", newEmail);
		url = url.replace("[appId]", appId);
		return url;
	}

	@Override
	@Transactional
	public String getMapitPublicAppId() {
		log.info("getMapitPublicAppId called..");
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredApp MapitApp = entityManager.createQuery("SELECT t FROM RegisteredApp t WHERE name=:name", RegisteredApp.class)
												.setParameter("name", "MAPIT")
												.getSingleResult();

		String appId = MapitApp.getPublicAppId();
		return appId;
	}

	
	@Override
	@Transactional
	public void setStatus(long id, EmailUpdateStatus status) {
		log.info("setStatus called.."+id+"||"+status);
		EntityManager entityManager = entityManagerProvider.get();
		EmailChangeTracker updateEmailUser = entityManager
				.createNamedQuery("EmailChangeTracker.findById", EmailChangeTracker.class).setParameter("id", id)
				.getSingleResult();
		updateEmailUser.setStatus(status);
	}

	@Override
	public UusResponseDto updateInMapit(String email, String newEmail) {
		log.info("UpdateInMapit called with email : "+email+" | newEmail : "+newEmail);
		CloseableHttpResponse response = null;
		try {
			String url = getMapitEmailUpdateUrl(email, newEmail);
			String domainUrl = ninjaProperties.get("mapit.client.domain.url");
			url = domainUrl + url;
			log.debug("Final MAPIT email Update Url \n "+url+"\n");
			HttpGet get = new HttpGet(url);
			response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();
			String m = EntityUtils.toString(entity);
			Gson g = new Gson();
			return g.fromJson(m, UusResponseDto.class);

		} catch (Exception e) {
			log.debug(e);
			log.info("error in mapit");
			throw new UUSException(email + " : can not change email in mapit");
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new UUSException(email + " : can not change email in mapit");
			} catch (Exception e) {
				e.printStackTrace();
				throw new UUSException(email + " : can not change email in mapit");
			}
		}
	}

	@Override
	public UusResponseDto updateInMatch(String email, String newEmail) {
		log.info("UpdateInMatch called with email : "+email+" | newEmail : "+newEmail);
		CloseableHttpResponse response = null;
		try {
			String url = getMatchEmailUpdateUrl(email, newEmail);
			String domainUrl = ninjaProperties.get("match.client.domain.url");
			url = domainUrl + url;
			log.debug("Final MATCH email Update Url \n "+url);
			HttpPut put = new HttpPut(url);
			response = httpClient.execute(put);
			HttpEntity entity = response.getEntity();
			String m = EntityUtils.toString(entity);
			Gson g = new Gson();
			response.close();
			log.info(m);
			return g.fromJson(m, UusResponseDto.class);

		} catch (Exception e) {
			log.debug(e);
			log.info("error in match");
			throw new UUSException(email + " : can not change email in match");
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new UUSException(email + " : can not change email in match");
			} catch (Exception e) {
				e.printStackTrace();
				throw new UUSException(email + " : can not change email in mapit");
			}
		}
	}

	@Override
	@Transactional
	public void FinalUpdateEmailStatus(long id, String email, String newEmail, String comment, long startTime) {
		long finalTime;
		log.info("FinalUpdateEmailStatus called..");
		if (id == -1) {
			setEmailStatus(email, newEmail, false, EmailUpdateStatus.FAILED, comment);
		} else {
			setStatus(id, EmailUpdateStatus.REVERTING);
			EntityManager entityManager = entityManagerProvider.get();
			EmailChangeTracker updateEmailUser = entityManager
					.createNamedQuery("EmailChangeTracker.findById", EmailChangeTracker.class).setParameter("id", id)
					.getSingleResult();
			if (updateEmailUser.getStatus() != EmailUpdateStatus.DONE) {
				AppStatusEntityDto appStatusEntityDto = updateEmailUser.getAppStatus();
				if (appStatusEntityDto != null) {
					Map<Servers, AppStatusUnitEntityDto> appStatusUnits = appStatusEntityDto.getAppStatusUnits();
					AppStatusUnitEntityDto mapitStatus = appStatusUnits.get(Servers.MAPIT);
					if (mapitStatus != null && mapitStatus.getStatus() == EmailUpdateStatus.DONE) {
						UusResponseDto uusResponseDto = updateInMapit(newEmail, email);
						finalTime = System.currentTimeMillis();
						updateEmailTrackingStatus(id, Servers.MAPIT, EmailUpdateStatus.REVERTED, uusResponseDto);
					}
					AppStatusUnitEntityDto matchStatus = appStatusUnits.get(Servers.MATCH);
					if (matchStatus != null && matchStatus.getStatus() == EmailUpdateStatus.DONE) {
						UusResponseDto uusResponseDto = updateInMatch(newEmail, email);
						finalTime = System.currentTimeMillis();
						updateEmailTrackingStatus(id, Servers.MATCH, EmailUpdateStatus.REVERTED, uusResponseDto);
					}
					AppStatusUnitEntityDto uusStatus = appStatusUnits.get(Servers.UUS);
					if (uusStatus != null && uusStatus.getStatus() == EmailUpdateStatus.DONE) {
						UusResponseDto uusResponseDto = updateEmailInUus(newEmail, email);
						finalTime = System.currentTimeMillis();
						updateEmailTrackingStatus(id, Servers.UUS, EmailUpdateStatus.REVERTED, uusResponseDto);
					}
				}
				setStatus(id, EmailUpdateStatus.REVERTED);
			}
		}
	}

	@Override
	@Transactional
	public void updateEmailTrackingStatus(long id, Servers server, EmailUpdateStatus emailUpdateStatus, UusResponseDto uusResponseDto) {
		log.info("updateEmailTrackingStatus called with id: " + id);
		EntityManager entityManager = entityManagerProvider.get();
		EmailChangeTracker updateEmailUser = entityManager
				.createNamedQuery("EmailChangeTracker.findById", EmailChangeTracker.class).setParameter("id", id)
				.getSingleResult();
		try {
			AppStatusUnitEntityDto appStatusUnitEntityDto = updateAppStatusWithResponseDto(emailUpdateStatus, uusResponseDto);
			updateEmailChangeTrackerWithAppStatus(server, updateEmailUser, appStatusUnitEntityDto);
		} catch (UUSException e) {
			throw new UUSException("cannot change email");
		}
	}

	private void updateEmailChangeTrackerWithAppStatus(Servers server, EmailChangeTracker updateEmailUser,
			AppStatusUnitEntityDto appStatusUnitEntityDto) {
		AppStatusEntityDto AppStatus = updateEmailUser.getAppStatus();
		if (AppStatus == null) {
			AppStatus = new AppStatusEntityDto();
			updateEmailUser.setAppStatus(AppStatus);
		}
		log.info("AppStatus :" + AppStatus.getAppStatusUnits());
		Map<Servers, AppStatusUnitEntityDto> appStatusUnits = new HashMap<>();
		if (AppStatus.getAppStatusUnits() != null) {
			appStatusUnits = AppStatus.getAppStatusUnits();
		}
		appStatusUnits.put(server, appStatusUnitEntityDto);
		AppStatus.setAppStatusUnits(appStatusUnits);
		updateEmailUser.setAppStatus(AppStatus);
	}

	private AppStatusUnitEntityDto updateAppStatusWithResponseDto(EmailUpdateStatus emailUpdateStatus, UusResponseDto uusResponseDto) {
		AppStatusUnitEntityDto appStatusUnitEntityDto = new AppStatusUnitEntityDto();
		String userIdFromOtherSystem = uusResponseDto.getId();
		boolean isSuccess = uusResponseDto.isSuccess();
		Long currentTime = System.currentTimeMillis();
		if (appStatusUnitEntityDto.getInitTime() == null) {
			appStatusUnitEntityDto.setInitTime(currentTime);
		}
		appStatusUnitEntityDto.setModifiedAt(currentTime);
		appStatusUnitEntityDto.setStatus(emailUpdateStatus);
		appStatusUnitEntityDto.setUserId(userIdFromOtherSystem);
		if (!isSuccess) {
			String error = "";
			if (uusResponseDto.getError() != null) {
				for (int i = 0; i < uusResponseDto.getError().size(); i++) {
					error = uusResponseDto.getError().get(i).getMessage() + ",";
				}
				appStatusUnitEntityDto.setMessage(error);
			}
		}
		return appStatusUnitEntityDto;
	}

	@Override
	@Transactional
	public UusResponseDto updateEmailInUus(String email, String newEmail) throws UUSException {
		log.info("updateEmailInUus called with email : " + email+ " | newEmail :"+newEmail);
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredAppUser> registeredUser = entityManager.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class)
															.setParameter("email", email)
															.getResultList();
		List<IntermediateData> intermediateUser = entityManager.createNamedQuery("IntermediateData.getUserByEmail", IntermediateData.class)
												 				.setParameter("email", email)
												 				.getResultList();
		
		if (registeredUser == null || registeredUser.size() != 1) {
			log.info("email id does not exist or exist in multipal row in registeredUser or intermediateUser  ");
			throw new UUSException(" error in updating email in Uus.");
		} else {
			log.info("registeredUser : "+registeredUser.size()+"\n intermediateUser : "+intermediateUser.size());
			return updateEmailInUus(email, newEmail, entityManager, registeredUser, intermediateUser);
		}
	}

	private UusResponseDto updateEmailInUus(String email, String newEmail, EntityManager entityManager,
			List<RegisteredAppUser> registeredUser, List<IntermediateData> intermediateUser) {
		log.info("updateEmailInUus" + email + "||" + newEmail);
		
		registeredUser.get(0).setEmail(newEmail.toLowerCase());
		entityManager.persist(registeredUser.get(0));
		if (intermediateUser != null && intermediateUser.size() > 0) {
			if (intermediateUser.get(0).getMapitEmail() != null && intermediateUser.get(0).getMatchEmail() != null
					&& (intermediateUser.get(0).getMapitEmail()).equals(email.toLowerCase())
					&& (intermediateUser.get(0).getMatchEmail()).equals(email.toLowerCase())) {
				intermediateUser.get(0).setMapitEmail(newEmail.toLowerCase());
				intermediateUser.get(0).setMatchEmail(newEmail.toLowerCase());
			} else if (intermediateUser.get(0).getMapitEmail() != null
					&& intermediateUser.get(0).getMapitEmail().equals(email.toLowerCase())) {
				intermediateUser.get(0).setMapitEmail(newEmail.toLowerCase());
			} else {
				intermediateUser.get(0).setMatchEmail(newEmail.toLowerCase());
			}
			entityManager.persist(intermediateUser.get(0));
		}
		long id = registeredUser.get(0).getUuid();
		return getUusStatus(id);
	}

	private UusResponseDto getUusStatus(long id) {
		log.info("getUusStatus called "+id);
		UusResponseDto uusResponseDto = new UusResponseDto();
		List<MessageDto> messageDtos = new ArrayList<MessageDto>();
		MessageDto messageDto = new MessageDto();
		uusResponseDto.setId(Long.toString(id));
		messageDto.setMessage("Success");
		messageDtos.add(messageDto);
		uusResponseDto.setError(messageDtos);
		return uusResponseDto;
	}

	@Override
	public void isValidRegisterRequest(String email) throws UUSException, UusInvalidUserException {
		if (!DtoUtils.isEmailValid(email)) {
			throw new InvalidEmailFormatException("Invalid email");
		}
		if (self.isUserAlreadyExists(email)) {
			throw new UusInvalidUserException("User is already registered");
		}
	}

	@Override
	public void isValidForgetPasswordRequest(String email) throws UUSException, UusInvalidUserException {
		if (!DtoUtils.isEmailValid(email)) {
			throw new InvalidEmailFormatException("Invalid email");
		}
		if (!self.isUserAlreadyExists(email)) {
			throw new UusInvalidUserException("User does not exist");
		}
	}

	@Override
	public boolean isValidAdminRegisterRequest(String publicAppId, String redirectTo, UserDto user, String privateAppId)
			throws UUSException {
		log.info("isValidAdminRegisterRequest called : redirectTo : " + redirectTo);

		if (DtoUtils.isStringEmptyOrNull(publicAppId) && DtoUtils.isStringEmptyOrNull(privateAppId)) {
			throw new UUSException("Please provide valid application-id");
		}
		return true;
	}

	@Override
	@Transactional
	public boolean isUserAlreadyExists(String email) {
		log.info("isUserAlreadyExists called : email : " + email);
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredAppUser> registeredUser = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();
		if (registeredUser != null && registeredUser.size() >= 1) {
			log.info("user exists : " + email);
			return true;
		}
		log.info("new user : " + email);
		return false;
	}

	@Override
	@Transactional
	public UserDto isUserAlreadyExistsWithDetails(String email) {
		log.info("isUserAlreadyExists called : email : " + email);
		if (!DtoUtils.isEmailValid(email)) {
			throw new InvalidEmailFormatException("Invalid email");
		}
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredAppUser> registeredUser = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();
		if (registeredUser.size() == 1) {
			RegisteredAppUser registeredAppUser = registeredUser.get(0);
			log.info("user exists : " + email);
			UserDto user = new UserDto();
			user.setEmail(email);
			user.setRole(registeredAppUser.getRoles().getRoles().get(0).toString());
			UserDetailsEntityDto userDetailsEntityDto = registeredAppUser.getUserDetails();
			if (userDetailsEntityDto == null) {
				userDetailsEntityDto = new UserDetailsEntityDto();
			}
			String name = userDetailsEntityDto.getName();
			if (name == null) {
				name = email.substring(0, email.indexOf('@'));
			}
			user.setName(name);
			return user;
		} else if (registeredUser.size() > 1) {
			log.error("**********************************************************");
			log.error("**** MULTIPLE USER WITH SAME NAME : " + email +" *********");
			log.error("**********************************************************");
			throw new UUSException("Multiple user with same email id : " + email);
		}
		log.info("new user : " + email);
		return null;
	}

	@Override
	@Transactional
	public void sendResetPasswordLink(String email, String publicAppId, String redirectLink) throws UUSEncryptionException {
		log.info("sendResetPasswordLink called email :" + email);
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredAppUser> registeredUserList = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();
		if (registeredUserList == null || registeredUserList.size() == 0) {
			log.info("User does not exist");
			throw new UUSException(email + " does not exist. Please ");
		}
		log.info("user exists : " + email);

		RegisteredAppUser registeredAppUser = registeredUserList.get(0);
		RegisteredAppDto registeredAppDto = new  RegisteredAppDto();
		String appName = "MATCH";
		if (registeredAppUser.getRegisteredApps() != null){
			if(registeredAppUser.getRegisteredApps().size() > 0){
				registeredAppDto = DtoUtils.mapToDto(registeredAppUser.getRegisteredApps().get(0));
				appName = registeredAppDto.getName();
			}
		} else {
			log.info("Fetching from RegisteredApp table.");
			List<RegisteredApp> registeredApps = entityManager
				.createNamedQuery("RegisteredApp.findByPublicAppId", RegisteredApp.class)
				.setParameter("publicAppId", publicAppId).getResultList();
				if(registeredApps.size() == 0){
					log.debug("---------------------------------------------------------------------");
					log.debug("---------------------------------------------------------------------");
					log.debug("---------------------------------------------------------------------");
					log.debug("--------------------- Registered Apps not found ---------------------");
					log.debug("---------------------------------------------------------------------");
					log.debug("---------------------------------------------------------------------");
					log.debug("---------------------------------------------------------------------");
					log.debug("ERROR: Cannot Reset Sub User Password");
				} else {
					registeredAppDto = DtoUtils.mapToDto(registeredApps.get(0));
					appName = registeredAppDto.getName();
				}
		}
		//RegisteredAppDto registeredAppDto = DtoUtils.mapToDto(registeredApps.get(0));
		String resetPasswordLink = generateResetPasswordLink(email, redirectLink, publicAppId,
				registeredAppUser.getUuid(), registeredAppUser.getHashedPassword());

		Map<String, String> scope = new HashMap<>();
		scope.put("username", email);
		scope.put("activationLink", resetPasswordLink);
		String htmlMail = DtoUtils.generateStringFromTemplate("views/email/resetPasswordMail.html", scope);
		String txtMail = "";// DtoUtils.generateStringFromTemplate("views/email/newUserVerification.txt",
							// scope);
		String fromEmail = "support@myanatomy.in";
		String toEmail = email;
		String subject = "Reset password " + appName;
		mailVerificationLink(toEmail, fromEmail, subject, htmlMail, txtMail, registeredAppDto, resetPasswordLink);
	}

	private String generateResetPasswordLink(String email, String redirectLink, String publicAppId, Long uuid, String password) throws UUSEncryptionException {
		log.info("generateResetPasswordLink called email : " + email);
		String domain = ninjaProperties.get("uus.domain");
		String resetPasswordLink = ninjaProperties.get("uus.resetPasswordLink");
		String validationHash = DtoUtils.getPasswordResetHash(email, publicAppId, uuid, password);
		resetPasswordLink = resetPasswordLink.replaceAll(":email", email).replaceAll(":publicAppId", publicAppId)
				.replaceAll(":redirectLink", redirectLink).replaceAll(":resetPasswordCodeHash", validationHash);
		return domain + resetPasswordLink;
	}

	@Override
	@Transactional
	public void isValidPasswordResetRequest(String email, String publicAppId, String validationHash) throws UUSEncryptionException, InvalidResetPasswordHash {
		log.info("isValidPasswordResetRequest called email : " + email);
		RegisteredAppUser registeredAppUser = self.getRegisteredUserByEmail(email);
		Long uuid = registeredAppUser.getUuid();
		String passwordResetHash = DtoUtils.getPasswordResetHash(email, publicAppId, uuid, registeredAppUser.getHashedPassword());
		if (!validationHash.equals(passwordResetHash)) {
			throw new InvalidResetPasswordHash("Invalid hash");
		}
	}

	@Override
	@Transactional
	public void updatePassword(UserDto userDtoWithOldPassword, String publicAppId) throws UUSEncryptionException {
		log.info("isValidPasswordUpdateRequest called email : " + userDtoWithOldPassword.getEmail());

		EntityManager entityManager = entityManagerProvider.get();
		String oldPassword = userDtoWithOldPassword.getOldPassword();
		String newPassword = userDtoWithOldPassword.getPassword();
		String email = userDtoWithOldPassword.getEmail();

		RegisteredAppUser registeredUser = getUserByEmailNPassword(entityManager, email, oldPassword);
		if (registeredUser == null) {
			throw new UUSException("Invalid email or password");
		}
		log.info("Is Valid request updating Password for email ");

		String hashedNewPassword = DtoUtils.getPasswordHash(email, newPassword);
		registeredUser.setHashedPassword(hashedNewPassword);
		registeredUser.setPasswordAsString(newPassword);
		registeredUser.setUserPasswordUpdated(true);
	}

	@Override
	@Transactional
	public void updatePasswordByAdmin(UserDto userDto, String publicAppId) throws UUSEncryptionException {
		RegisteredAppUser registeredUser = self.getRegisteredUserByEmail(userDto.getEmail());
		String hashedPassword = DtoUtils.getPasswordHash(userDto.getEmail(), userDto.getPassword());
		registeredUser.setHashedPassword(hashedPassword);
		registeredUser.setPasswordAsString(userDto.getPassword());
		registeredUser.setUserPasswordUpdated(true);
	}

	@Override
	@Transactional
	public void resetNewPassword(UserDto user) throws UUSEncryptionException {
		log.info("resetNewPassword called " + user.getEmail());
		EntityManager em = entityManagerProvider.get();
		String email = user.getEmail();
		String password = user.getPassword();
		RegisteredAppUser registeredUser = self.getRegisteredUserByEmail(email);
		try {
			String hashedPassword = DtoUtils.getPasswordHash(email, password);
			registeredUser.setHashedPassword(hashedPassword);
			registeredUser.setUserPasswordUpdated(true);
			registeredUser.setPasswordAsString(password);
			em.createQuery("Delete ValidUserSessionToken x where x.uuid = :uuid")
					.setParameter("uuid", registeredUser.getUuid()).executeUpdate();
			log.info("Password Reset Successfully");
		} catch (UUSEncryptionException e) {
			throw new UUSEncryptionException("Password hash could not be generated");
		}
	}

	@Override
	@Transactional
	public BulkRegisterResponseDto inviteCandidate(List<String> emails, String publicAppId, String redirectLink,
			String privateAppId) throws Exception {

		EntityManager em = entityManagerProvider.get();
		BulkRegisterResponseDto bulkRegisterDto = new BulkRegisterResponseDto();
		List<UserDto> invitedNRegistereds = new ArrayList<>();
		List<RejectedUserDto> rejectedUserList = new ArrayList<>();

		for (String email : emails) {
			if (!DtoUtils.isEmailValid(email)) {
				RejectedUserDto rejectedUserDto = new RejectedUserDto();
				rejectedUserDto.setEmail(email);
				rejectedUserDto.setReason(RejectedReason.INVALID);
				rejectedUserList.add(rejectedUserDto);
				emails.remove(email);
			}
		}

		List<RegisteredApp> registeredApps = em
				.createNamedQuery("RegisteredApp.findByPublicAppIdOrPrivateAppId", RegisteredApp.class)
				.setParameter("publicAppId", publicAppId).setParameter("privateAppId", privateAppId).getResultList();

		for (String email : emails) {
			List<RegisteredAppUser> registeredUserList = em
					.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class)
					.setParameter("email", email).getResultList();
			if (registeredUserList == null || registeredUserList.size() == 0) {
				log.info("A new user with email :" + email + "has been created");
				String randomString = DtoUtils.randomString(6);

				RegisteredAppUser newUser = new RegisteredAppUser();
				newUser.setEmail(email.toLowerCase());
				newUser.setHashedPassword(DtoUtils.getPasswordHash(email, randomString));
				newUser.setPasswordAsString(randomString);
				RolesEntityDto rolesEntityDto = new RolesEntityDto();
				rolesEntityDto.addRole(UserRole.valueOf("CANDIDATE"));
				newUser.setRoles(rolesEntityDto);
				newUser.setRegisteredApps(registeredApps);
				em.persist(newUser);

				UserDto user = new UserDto();
				user.setEmail(email.toLowerCase());
				user.setRole(UserRole.valueOf("CANDIDATE").toString());
				user.setPassword(randomString);
				user.setNewUser(true);
				invitedNRegistereds.add(user);
			} else {
				RegisteredAppUser registeredUser = registeredUserList.get(0);
				if (registeredUser.getRoles().getRoles().get(0) != UserRole.CANDIDATE) {
					log.info("Existing User found is not a Candidate. So Rejecting the following users");
					RejectedUserDto rejectedUserDto = new RejectedUserDto();
					rejectedUserDto.setEmail(email);
					rejectedUserDto.setReason(RejectedReason.HR);
					rejectedUserList.add(rejectedUserDto);
					continue;
				}
				log.info("Found Existing Candidate to Invite.");
				UserDto user = new UserDto();
				user.setRole(registeredUser.getRoles().getRoles().get(0).toString());
				user.setEmail(email.toLowerCase());
				user.setNewUser(false);
				invitedNRegistereds.add(user);
			}
		}
		bulkRegisterDto.setInviteNRegister(invitedNRegistereds);
		bulkRegisterDto.setRejectedUsers(rejectedUserList);
		return bulkRegisterDto;
	}

	@Override
	@Transactional
	public void userExistAsCandidate(List<String> emails) throws CandidateAlreadyExist {
		log.info("userExistAsCandidate called");
		EntityManager em = entityManagerProvider.get();
		List<RegisteredAppUser> appUsersList = em
				.createNamedQuery("RegisteredAppUser.findCandidateByEmails", RegisteredAppUser.class)
				.setParameter("emails", emails).getResultList();
		for (RegisteredAppUser user : appUsersList) {
			List<UserRole> roles = user.getRoles().getRoles();
			for (UserRole role : roles) {
				if (role == UserRole.CANDIDATE) {
					throw new CandidateAlreadyExist("The User Exist As A Candidate");
				}
			}
		}
	}

	@Override
	@Transactional
	public void createUser(UserDto userDto, String publicAppId, String privateAppId) throws Exception {
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredAppUser user = new RegisteredAppUser();
		user.setEmail(userDto.getEmail().toLowerCase());
		user.setHashedPassword(DtoUtils.getPasswordHash(userDto.getEmail(), userDto.getPassword()));
		user.setPasswordAsString(userDto.getPassword());
		RolesEntityDto rolesEntityDto = new RolesEntityDto();
		rolesEntityDto.addRole(UserRole.valueOf(userDto.getRole()));
		user.setRoles(rolesEntityDto);

		ServicedAppEntityDto servicedAppEntityDto = new ServicedAppEntityDto();
		servicedAppEntityDto.addAppId(publicAppId);

		List<RegisteredApp> registeredApps = entityManager
				.createNamedQuery("RegisteredApp.findByPublicAppIdOrPrivateAppId", RegisteredApp.class)
				.setParameter("publicAppId", publicAppId).setParameter("privateAppId", privateAppId).getResultList();
		user.setRegisteredApps(registeredApps);
		UserDetailsEntityDto userDetailsEntityDto = new UserDetailsEntityDto();
		userDetailsEntityDto.setName(user.getEmail());
		user.setUserDetails(userDetailsEntityDto);
		entityManager.persist(user);
		user.setActive(true);
	}

	@Override
	@Transactional
	public boolean getIntermediateUser(String email, String password) {
		EntityManager em = entityManagerProvider.get();
		List<IntermediateData> intermediateUser = em
				.createNamedQuery("IntermediateData.getUserByEmail", IntermediateData.class)
				.setParameter("email", email).getResultList();
		if (intermediateUser != null && intermediateUser.size() == 1) {
			IntermediateData user = intermediateUser.get(0);
			if (user.getMapitEmail() != null) {
				log.info("Mapit User Found");
				String mapitPassword = DtoUtils.encryptMd5(password);
				if (user.getMapitPasswordHash().equals(mapitPassword)) {
					return true;
				}
				log.info("Incorrect Mapit Password Provided");
			}
			if (user.getMatchEmail() != null) {
				log.info("Match User Found");
				String matchPassword = DtoUtils.generateMatchPasswordHash(password, user.getMatchSalt());
				if ((user.getMatchPasswordHash().equals(matchPassword))) {
					return true;
				}
				log.info("Incorrect Match Password Provided");
			}
		}
		return false;
	}

	@Override
	@Transactional
	public BulkRegisterResponseDto createUsersInBulk(List<UserDto> list, String publicAppId, String privateAppId,
			String redirectLink) throws Exception {
		EntityManager entityManager = entityManagerProvider.get();
		List<RejectedUserDto> rejectedUserList = new ArrayList<>();
		for (UserDto user : list) {
			if (!DtoUtils.isEmailValid(user.getEmail())) {
				RejectedUserDto rejectedUserDto = new RejectedUserDto();
				rejectedUserDto.setEmail(user.getEmail());
				rejectedUserDto.setReason(RejectedReason.INVALID);
				rejectedUserList.add(rejectedUserDto);
				list.remove(user);
			}
			if (isUserAlreadyExists(user.getEmail())) {
				RejectedUserDto rejectedUserDto = new RejectedUserDto();
				rejectedUserDto.setEmail(user.getEmail());
				rejectedUserDto.setReason(RejectedReason.DUPLICATED);
				rejectedUserList.add(rejectedUserDto);
				list.remove(user);
			}
		}

		List<UserDto> createdUsers = new ArrayList<>();

		List<RegisteredApp> registeredApps = entityManager
				.createNamedQuery("RegisteredApp.findByPublicAppIdOrPrivateAppId", RegisteredApp.class)
				.setParameter("publicAppId", publicAppId).setParameter("privateAppId", privateAppId).getResultList();
		RegisteredAppDto registeredAppDto = DtoUtils.mapToDto(registeredApps.get(0));

		for (UserDto user : list) {
			boolean createdAsSubUser = false;
			RegisteredAppUser registeredUser = self.createUserWithoutPassword(user, registeredApps, createdAsSubUser);
			String resetPasswordLink = getResetPasswordActivationLink(user.getEmail(), registeredAppDto.getAppSecret(),
					registeredUser.getUuid(), registeredAppDto, redirectLink, registeredUser.getHashedPassword());
			Map<String, String> scope = new HashMap<>();
			scope.put("activationLink", resetPasswordLink);
			scope.put("username", user.getEmail());
			log.info(resetPasswordLink);
			String htmlMail = DtoUtils.generateStringFromTemplate("views/email/resetPasswordMail.html", scope);
			String txtMail = "";
			String fromEmail = "support@myanatomy.in";
			String toEmail = user.getEmail();
			String subject = "Welcome to " + registeredAppDto.getName();
			mailVerificationLink(toEmail, fromEmail, subject, htmlMail, txtMail, registeredAppDto, resetPasswordLink);
			UserDto userDto = new UserDto();
			userDto.setEmail(user.getEmail().toLowerCase());
			userDto.setRole(user.getRole());
			createdUsers.add(userDto);
		}

		BulkRegisterResponseDto bulkRegisterResponseDto = new BulkRegisterResponseDto();
		bulkRegisterResponseDto.setRejectedUsers(rejectedUserList);
		bulkRegisterResponseDto.setInviteNRegister(createdUsers);
		return bulkRegisterResponseDto;
	}

	@Override
	public boolean isValidEmailChangeRequest(String fromEmail, String toEmail) {
		log.debug("isValidEmailChangeRequest called fromEmail : " + fromEmail + " || toEmail : " + toEmail);
		self.validateFromEmailId(fromEmail);
		self.validateToEmail(toEmail);
		return true;
	}

	@Override
	public void validateFromEmailId(String fromEmail) {
		log.info("isValidFromEmailId called fromEmail : " + fromEmail);
		boolean isEmailExists = self.isRegisterUserExistByEmail(fromEmail);
		if (!isEmailExists) {
			log.info("Email does not  exist : " + fromEmail);
			throw new InvalidEmailException("Email does not  exist : " + fromEmail);
		}
	}

	@Override
	public void validateToEmail(String toEmail) {
		log.info("isValidToEmail called toEmail : " + toEmail);
		boolean isValidEmailFormat = DtoUtils.isEmailValid(toEmail);
		if (!isValidEmailFormat) {
			log.info("Invalid Email Format");
			throw new InvalidEmailException("Invalid email format : " + toEmail);
		}
		boolean isEmailExists = self.isRegisterUserExistByEmail(toEmail);
		if (isEmailExists) {
			log.info("Email already exists : " + toEmail);
			throw new InvalidEmailException("Email already exists : " + toEmail);
		}
	}

	@Override
	@Transactional
	public boolean isRegisterUserExistByEmail(String email) throws InvalidEmailException {
		log.info("isRegisterUserExistByEmail called : " + email);
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredAppUser> registeredUser = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();

		if (registeredUser != null && registeredUser.size() == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	@Transactional
	public boolean isValidSubUserEmail(String email) {
		log.info("isValidSubUserEmail called with email: " + email);
		EntityManager entityManager = entityManagerProvider.get();
		boolean validSubUserEmail = false;
		boolean isUserCandidate = false;
		
		List<RegisteredAppUser> registeredUser = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();
		
		if (registeredUser != null && registeredUser.size() == 1) {
			RolesEntityDto registeredUserRoles = registeredUser.get(0).getRoles();
			for(UserRole userRole : registeredUserRoles.getRoles()) {
				if(userRole.equals(UserRole.CANDIDATE)) {
					log.info("Email " + email +" found as Candidate");
					isUserCandidate = true;
					break;
				}
			}
			if(!isUserCandidate) {
				log.info("Email " + email +" is not Candidate");
				validSubUserEmail = true;
			}
		}
		
		return validSubUserEmail;
	}
	
	@Override
	public boolean isValidSubUserId(Long uuid) {
		log.info("isValidSubUserId called with uuid: " + uuid);
		
		boolean isUserCandidate = false;
		boolean validSubUserId = false;
		
		if(uuid == null) {
			return validSubUserId;
		}
		
		RegisteredAppUser registeredUser = self.getRegisteredAppUserByUuid(uuid);
		
		if(registeredUser != null) {
			RolesEntityDto registeredUserRoles = registeredUser.getRoles();
			for(UserRole userRole : registeredUserRoles.getRoles()) {
				if(userRole.equals(UserRole.CANDIDATE)) {
					log.info("Uuid " + uuid +" found as Candidate");
					isUserCandidate = true;
					break;
				}
			}
			if(!isUserCandidate) {
				log.info("Uuid " + uuid +" is not Candidate");
				validSubUserId = true;
			}
		}
		
		return validSubUserId;
	}
	
	@Override
	@Transactional
	public RegisteredAppUser getRegisteredAppUserByUuid(Long uuid) {
		log.info("getRegisteredAppUserByUuid called with uuid: " + uuid);
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredAppUser registeredAppUser = entityManager.find(RegisteredAppUser.class, uuid);
		return registeredAppUser;
	}
	
	@Override
	@Transactional
	public boolean isSubUserExistByEmail(String email) {
		log.info("isValidSubUserEmail called with email: " + email);
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredAppUser> registeredUser = entityManager
				.createNamedQuery("RegisteredAppUser.findByEmail", RegisteredAppUser.class).setParameter("email", email)
				.getResultList();
		
		if(registeredUser == null || registeredUser.size() == 0) {
			return false;
		} 
		
		return true;
	}
	
	@Override
	@Transactional
	public List<RegisteredApp> getRegisteredAppsByName(String name) {
		log.info("getRegisteredAppsByName called With name : "+name);
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredApp> registeredApps = entityManager.createNamedQuery("RegisteredApp.findByName",RegisteredApp.class)
				.setParameter("name", name)
				.getResultList();
		return registeredApps;
		
	}

	@Override
	public void createSubUserWithMinimalDetailsByEmail(String email, String userName) throws UUSEncryptionException {
		log.info("createSubUserWithMinimalDetailsByEmail called With email : "+email +" | userName : " + userName);
		
		boolean isValidEmail = DtoUtils.isEmailValid(email);
		if(!isValidEmail) {
			log.info("Emailid "+email+" is not valid");
			throw new InvalidEmailException("Not a valid Email id to create Sub User");
		}
		
		String appName = "match";
		List<RegisteredApp> registeredApps = self.getRegisteredAppsByName(appName);
		RegisteredAppDto registeredAppDto = DtoUtils.mapToDto(registeredApps.get(0));
		
		userName = (userName == null || userName.length() == 0) ? DtoUtils.getNameFromEmail(email) : userName;
		String role = UserRole.CORPORATE.toString();
		String redirectLink = "https://match.myanatomy.in";
		
		boolean createdAsSubUser = true;
		Long superUsersCount = 1l;
		
		UserDto userDto = new UserDto();
		userDto.setEmail(email);
		userDto.setName(userName);
		userDto.setContactNumber("");
		userDto.setRole(role);
		userDto.setCreatedAsSubUser(createdAsSubUser);
		userDto.setSuperUsersCount(superUsersCount);
		RegisteredAppUser registeredUser = self.createUserWithoutPassword(userDto, registeredApps, createdAsSubUser);
		log.info("User created with email : "+registeredUser.getEmail() +" | Name : " + registeredUser.getUserDetails().getName());
		
		sendResetPasswordLinkForNewSubUser(userDto, registeredAppDto, registeredUser, redirectLink);
	}
	
	private void sendResetPasswordLinkForNewSubUser(UserDto userDto, RegisteredAppDto registeredAppDto, 
			RegisteredAppUser registeredUser, String redirectLink) {
		log.info("sendResetPasswordLinkForNewSubUser is Called ");
		String resetPasswordLink = getResetPasswordActivationLink(userDto.getEmail(), registeredAppDto.getAppSecret(),
				registeredUser.getUuid(), registeredAppDto, redirectLink, registeredUser.getHashedPassword());
		Map<String, String> scope = new HashMap<>();
		scope.put("activationLink", resetPasswordLink);
		scope.put("username", userDto.getName());
		log.info(resetPasswordLink);
		String htmlMail = DtoUtils.generateStringFromTemplate("views/email/newUserRegistration.html", scope);
		String txtMail = "";
		String fromEmail = "support@myanatomy.in";
		String toEmail = userDto.getEmail();
		String subject = "Welcome to " + registeredAppDto.getName();
		mailVerificationLink(toEmail, fromEmail, subject, htmlMail, txtMail, registeredAppDto, resetPasswordLink);
		
	}
	
	@Override
	public String getSubUserToken(String ownerUusToken, Long subUserUuid) {
		log.info("getSubUserToken is called with SubUserId : "+subUserUuid);
		boolean isValidSubUserId = self.isValidSubUserId(subUserUuid);
		if(!isValidSubUserId) {
			log.info("Not a valid SubUser Uuid");
			throw new UUSException("Sub User Doesn't Exist or Sub User is Candidate");
		}
		TokenDto ownerTokenDto = tokenFacade.getTokenDtoByToken(ownerUusToken);
		Long ownerUuid = ownerTokenDto.getUuid();
		
		AccessControlListDto aclDto = aclFacade.getACLByOwnerNdSubUserIdsForSubUserToken(subUserUuid, ownerUuid); 
		//Owner becomes Sub User and Vice versa
		
		SubUserLogInDto subUserLoginDto = DtoUtils.createSubUserLogInDto(aclDto, ownerTokenDto);
		subUserLoginDto.setTokenType(UusTokenTypeDto.SUB_USER);
		
		String encryptedObject = encryptSubUserLogInDto(subUserLoginDto);
		
		return encryptedObject;
	}

	private String encryptSubUserLogInDto(SubUserLogInDto subUserLoginDto) {
		
		SubUserEncryptedDto subUserEncryptedDto = getSubUserEncryptedDto(subUserLoginDto);
		String subUserEncryptedDtoJson = getJsonFromObject(subUserEncryptedDto);
		String encryptedObject = encryptWithAES(subUserEncryptedDtoJson);
		
		return encryptedObject;
	}
	
	@Override
	public boolean validateSubUserActionWithToken(String subUserToken, String accessArea, String action) 
			throws InvalidAccessArea, InvalidAccessPermission, InvalidUusTokenType {
		log.info("validateSubUserActionWithToken is called with accessArea : "+accessArea+" | action : "+action);
		
		validateAccessAreaNdAction(accessArea, action);
		
		SubUserLogInDto subUserLoginDto = decryptSubUserTokenToSubUserLoginDto(subUserToken);
		
		AccessAreaDto area = AccessAreaDto.valueOf(accessArea);
		AccessPermissionDto accessPermission = AccessPermissionDto.valueOf(action);
		
		boolean isValidAction = false;
		isValidAction = validateActionForAcessArea(subUserLoginDto, area, accessPermission);
		
		return isValidAction;
	}

	private SubUserLogInDto decryptSubUserTokenToSubUserLoginDto(String subUserToken) throws InvalidUusTokenType {
		SubUserEncryptedDto subUserEncryptedDto = new SubUserEncryptedDto();
		MinimalUserDetailsDto ownerDetails = new MinimalUserDetailsDto();
		SubUserDetailsDto subUserDetails = new SubUserDetailsDto();
		
		String decryptedJsonObject = decryptWithAES(subUserToken);
		
		subUserEncryptedDto = (SubUserEncryptedDto)getObjectFromJson(decryptedJsonObject, SubUserEncryptedDto.class);
		if(subUserEncryptedDto == null) {
			log.info("Token Details are null");
			throw new UUSException("Sub User Token is not Valid");
		}
		
		String uusTokenType = decryptWithAES(subUserEncryptedDto.getTokenType());
		validateUusTokenType(uusTokenType);
		
		String ownerDetailsJson = decryptWithAES(subUserEncryptedDto.getOwnerDetails());
		String subUserDetailsJson = decryptWithAES(subUserEncryptedDto.getSubUserDetails());
		Long aclId = Long.parseLong(decryptWithAES(subUserEncryptedDto.getAclId()));
		Long refreshCounter = Long.parseLong(decryptWithAES(subUserEncryptedDto.getRefreshCounter()));
		
		ownerDetails = (MinimalUserDetailsDto)getObjectFromJson(ownerDetailsJson, MinimalUserDetailsDto.class);
		subUserDetails = (SubUserDetailsDto)getObjectFromJson(subUserDetailsJson, SubUserDetailsDto.class);
		
		SubUserLogInDto subUserLoginDto = new SubUserLogInDto();
		subUserLoginDto.setTokenType(UusTokenTypeDto.valueOf(uusTokenType));
		subUserLoginDto.setOwnerDetails(ownerDetails);
		subUserLoginDto.setSubUserDetails(subUserDetails);
		subUserLoginDto.setAclId(aclId);
		subUserLoginDto.setRefreshCounter(refreshCounter);
		return subUserLoginDto;
	}

	private SubUserEncryptedDto getSubUserEncryptedDto(SubUserLogInDto subUserLogInDto) {
		log.info("getSubUserEncryptedDto is called");
		SubUserEncryptedDto subUserEncryptedDto = new SubUserEncryptedDto();
		
		String encryptedUusTokenType = encryptWithAES(subUserLogInDto.getTokenType().toString());
		
		String ownerDetailsJson = getJsonFromObject(subUserLogInDto.getOwnerDetails());
		log.info("ownerDetailsJson : "+ownerDetailsJson);
		String subUserDetailsDtoJson = getJsonFromObject(subUserLogInDto.getSubUserDetails());
		log.info("subUserDetailsDtoJson : "+subUserDetailsDtoJson);
		
		String encryptedOwnerDetails = encryptWithAES(ownerDetailsJson);
		String encryptedsubUserDetails = encryptWithAES(subUserDetailsDtoJson);
		String encryptedAclId = encryptWithAES(subUserLogInDto.getAclId().toString());
		String encryptedRefreshCounter = encryptWithAES(subUserLogInDto.getRefreshCounter().toString());
		
		log.info("Refresh Counter : "+subUserLogInDto.getRefreshCounter());
		subUserEncryptedDto.setTokenType(encryptedUusTokenType);
		subUserEncryptedDto.setAclId(encryptedAclId);
		subUserEncryptedDto.setOwnerDetails(encryptedOwnerDetails);
		subUserEncryptedDto.setSubUserDetails(encryptedsubUserDetails);
		subUserEncryptedDto.setRefreshCounter(encryptedRefreshCounter);
		
		return subUserEncryptedDto;
	}

	private String getJsonFromObject(Object object) {
		log.info("getJsonFromObject is called with Object of class : "+object.getClass());
		String jsonFormat = "";
		if(object instanceof MinimalUserDetailsDto) {
			MinimalUserDetailsDto minimalUserDetailsDto = (MinimalUserDetailsDto)object;
			jsonFormat = g.toJson(minimalUserDetailsDto);
		} else if(object instanceof SubUserLogInDto) {
			SubUserLogInDto subUserLogInDto = (SubUserLogInDto)object;
			jsonFormat = g.toJson(subUserLogInDto);
		} else if(object instanceof SubUserDetailsDto) {
			SubUserDetailsDto subUserDetailsDto = (SubUserDetailsDto)object;
			jsonFormat = g.toJson(subUserDetailsDto);
		} else if(object instanceof SubUserEncryptedDto) {
			SubUserEncryptedDto subUserEncryptedDto = (SubUserEncryptedDto)object;
			jsonFormat = g.toJson(subUserEncryptedDto);
		}
		return jsonFormat;
	}
	
	private Object getObjectFromJson(String jsonString, Class<?> className) {
		log.info("getObjectFromJson is called with className : "+ className.getName());
		
		if(DtoUtils.areSameClasses(className, MinimalUserDetailsDto.class)) {
			return g.fromJson(jsonString, MinimalUserDetailsDto.class);
		} else if(DtoUtils.areSameClasses(className, SubUserLogInDto.class)) {
			return g.fromJson(jsonString, SubUserLogInDto.class);
		} else if(DtoUtils.areSameClasses(className, SubUserDetailsDto.class)) {
			return g.fromJson(jsonString, SubUserDetailsDto.class);
		} else if(DtoUtils.areSameClasses(className, SubUserEncryptedDto.class)) {
			return g.fromJson(jsonString, SubUserEncryptedDto.class);
		}
		log.info("returning null object");
		return null;
	}
	private String encryptWithAES(String stringToEncrypt) {
		log.info("encryptWithAES is called");
		String encryptedString = DtoUtils.encryptAES(ninjaProperties, stringToEncrypt);
		return encryptedString;
	}
	
	private String decryptWithAES(String stringToDecrypt) {
		log.info("decryptWithAES is called ");
		String decryptedString = DtoUtils.decryptAES(ninjaProperties, stringToDecrypt);
		return decryptedString;
	}
	
	private void validateAccessAreaNdAction(String accessArea, String action) throws InvalidAccessArea, InvalidAccessPermission {
		log.info("validateAccessAreaNdAction is called with accessArea : "+accessArea+" | action : "+action);
		boolean isValidAccessArea = DtoUtils.isValidaAccessArea(accessArea);
		if(!isValidAccessArea) {
			log.error("Error : Invalid Access Area : "+accessArea);
			throw new InvalidAccessArea(accessArea+" is not a Valid Access area");
		}
		
		boolean isValidAccessPermission = DtoUtils.isValidaAccessPermission(action);
		if(!isValidAccessPermission) {
			log.error("Error : Invalid Access Permission : "+action);
			throw new InvalidAccessPermission(action+" is not a Valid Action");
		}
	}
	
	private void validateUusTokenType(String uusTokenType) throws InvalidUusTokenType {
		log.info("validateUusTokenType is called with uusTokenType : "+uusTokenType);
		boolean isValidUusTokenType = DtoUtils.isValidUusTokenType(uusTokenType);
		if(!isValidUusTokenType) {
			log.error("Error : Invalid UusToken Type : "+uusTokenType);
			throw new InvalidUusTokenType("Invalid Uus Token Type");
		}
	}
	
	private boolean validateActionForAcessArea(SubUserLogInDto subUserLoginDto, AccessAreaDto area,
			AccessPermissionDto accessPermission) {
		log.info("validateActionForAcessArea is called with area : "+area+" | accessPermission : "+accessPermission);
		
		boolean isValidAction = false;
		if(subUserLoginDto.getSubUserDetails() == null || subUserLoginDto.getOwnerDetails() == null) {
			return isValidAction;
		}
		
		Long aclId = subUserLoginDto.getAclId();
		MinimalACLWebDto minimalAclwebDto = getMinimalAclWebDto(aclId);
		
		if(minimalAclwebDto == null) {
			throw new exceptions.AclNotFoundException("No Access were granted to user");
		}
		
		List<AccessControlUnitDto> permissionDetails = minimalAclwebDto.getAclUnits();
		
		for(AccessControlUnitDto accessControlUnit : permissionDetails) {
			List<AccessPermissionDto> permissions = accessControlUnit.getPermissions();
			boolean isValidPermssion = permissions.contains(accessPermission);
			if(area == accessControlUnit.getArea() && isValidPermssion) {
				log.info("Its a Valid Action by User");
				isValidAction = true;
				break;
			}
		}
		return isValidAction;
	}
	
	private MinimalACLWebDto getMinimalAclWebDto(Long aclId) {
		log.info("getMinimalAclWebDto is called with AclId : "+aclId);
		MinimalACLWebDto minimalAclwebDto = new MinimalACLWebDto();
		minimalAclwebDto = aclFacade.getAclFromCache(aclId);
		return minimalAclwebDto;
	}
	
	@Override
	public String refreshSubUserToken(String subUserToken) throws InvalidUusTokenType, AclNotFoundException {
		log.info("refreshSubUserToken is Called");
		SubUserLogInDto subUserLoginDto = decryptSubUserTokenToSubUserLoginDto(subUserToken);
		isValidSubUserToken(subUserLoginDto.getAclId());
		Long refreshCounter = subUserLoginDto.getRefreshCounter();
		subUserLoginDto.setRefreshCounter(++refreshCounter);
		String encryptedToken = encryptSubUserLogInDto(subUserLoginDto);
		
		return encryptedToken;
	}
	
	private void isValidSubUserToken(Long aclId) throws AclNotFoundException {
		log.info("isValidSubUserToken is called with aclId : "+aclId);
		MinimalACLWebDto minimalAclWebDto = aclFacade.getAclFromCache(aclId);
		if(minimalAclWebDto == null) {
			throw new exceptions.AclNotFoundException("No Access were granted to user");
		}
	}
}
