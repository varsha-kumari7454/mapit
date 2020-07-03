package controllers;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

import dto.BulkUserDto;
import dto.LinkDto;
import dto.UserDto;
import dto.UusResponseDto;
import exceptions.AclNotFoundException;
import exceptions.InvalidAccessArea;
import exceptions.InvalidAccessPermission;
import exceptions.InvalidActivationHash;
import exceptions.InvalidEmailException;
import exceptions.InvalidEmailFormatException;
import exceptions.InvalidResetPasswordHash;
import exceptions.InvalidUusTokenType;
import exceptions.UUSEncryptionException;
import exceptions.UUSException;
import exceptions.UserNotActiveException;
import exceptions.UusInvalidPublicAppId;
import exceptions.UusInvalidPublicOrPrivateAppId;
import exceptions.UusInvalidUserException;
import facade.AppFacade;
import facade.TokenFacade;
import facade.UserFacade;
import filters.RequestTrackFilter;
import models.EmailUpdateStatus;
import models.Servers;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.Context;
import ninja.params.Param;
import ninja.utils.NinjaProperties;

@FilterWith(RequestTrackFilter.class)
public class LoginRedirectControllerImpl implements LoginRedirectController {
	private static Logger log = LogManager.getLogger(LoginRedirectControllerImpl.class);

	@Inject
	private UserFacade userFacade;
	@Inject
	private TokenFacade tokenFacade;
	@Inject
	private AppFacade appFacade;
	@Inject
	private ControllerUtils controllerUtils;
	@Inject
	private CloseableHttpClient httpClient;
	@Inject
	NinjaProperties ninjaProperties;

	@Override
	public Result registerUser(UserDto user, @Param("redirectLink") String redirectTo,
			@Param("publicAppId") String publicAppId, @Param("privateAppId") String privateAppId,
			@Param("noRedirect") Boolean noRedirect, @Param("noMail") Boolean noMail,@Param("isReqByAdmin") Boolean isReqByAdmin) {
		log.info("registerUser called for :" + user.getEmail());
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			userFacade.isValidRegisterRequest(user.getEmail());
			String activationCodeHash = userFacade.registerUser(user, publicAppId, redirectTo, privateAppId, noMail, isReqByAdmin);
			if (noMail) {
				LinkDto activateLink = new LinkDto();
				activateLink.setLink(activationCodeHash);
				return Results.ok().json().render(activateLink);
			} else {
				return Results.ok().json().render("Please check your mail for activation link");
			}
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage());
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage());
		} catch (InvalidEmailFormatException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_EMAIL).json().render(e.getMessage());
		} catch (UusInvalidUserException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.USER_ALREADY_EXIST).json().render(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REGISTRATION).json().render(e.getMessage());
		}
	}

	@Override
	public Result registerNActivateUser(UserDto user, @Param("publicAppId") String publicAppId,
			@Param("privateAppId") String privateAppId, @Param("noRedirect") boolean noRedirect) {
		log.info("registerNActivateUser called for :" + user.getEmail());
		if (!appFacade.isValidPublicOrPrivateAppId(publicAppId, privateAppId, noRedirect)) {
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json()
					.render("Invalid public or private app id provided");
		}
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			userFacade.isValidRegisterRequest(user.getEmail());
			userFacade.createUser(user, publicAppId, privateAppId);

			return Results.ok().json().render("Successfully Created the User");
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage());
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage());
		} catch (InvalidEmailFormatException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_EMAIL).json().render(e.getMessage());
		} catch (UusInvalidUserException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.USER_ALREADY_EXIST).json().render(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return Results.badRequest().json().render("Invalid registration");
		}
	}

	@Override
	public Result adminRegisteringUserWithoutPassword(UserDto user, @Param("redirectLink") String redirectTo,
			@Param("privateAppId") String privateAppId, @Param("noRedirect") Boolean noRedirect,
			@Param("publicAppId") String publicAppId, @Param("sendMail") Boolean sendMail) {
		log.error("redirectTo : " + redirectTo);
		if (!appFacade.isValidPublicOrPrivateAppId(publicAppId, privateAppId, noRedirect)) {
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json()
					.render("Invalid public or private app id provided");
		}
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			UserDto userDto = userFacade.isUserAlreadyExistsWithDetails(user.getEmail());
			if (userDto != null) {
				return Results.status(ResponseStatus.INVALID_REQUEST).json().render(userDto);
			}
			userFacade.adminRegisteringUser(user, redirectTo, privateAppId, sendMail);
			return Results.ok().json().render("Registered successfully");
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage());
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage());
		} catch (UUSException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch (InvalidEmailFormatException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_EMAIL).json().render(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return Results.badRequest().json().render("Could not register user. Please try again");
		}
	}

	@Override
	public Result activateUser(@Param("activationCodeHash") String activationCodeHash,
			@Param("publicAppId") String publicAppId, @Param("privateAppId") String privateAppId,
			@Param("email") String email, @Param("redirectLink") String redirectLink,
			@Param("noRedirect") Boolean noRedirect) {
		log.info("activateUser called : redirectLink : " + redirectLink + " email: " + email);
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			userFacade.isValidUserActivationHash(activationCodeHash, publicAppId, email);

			boolean isActiveUser = true;
			userFacade.updateUserActivationStatus(email, isActiveUser);
			Long expiryDuration = 600000l;
			UserDto userDtoWithToken = tokenFacade.createUserToken(email, expiryDuration);
			if (noRedirect != null && noRedirect == false) {
				String redirectTo = redirectLink + "/" + email + "/" + userDtoWithToken.getToken();
				return Results.redirect(redirectTo);
			}
			return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(userDtoWithToken));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (InvalidActivationHash e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_HASH).json().render(e.getMessage()));
		} catch (UUSEncryptionException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch (UserNotActiveException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
	}

	@Override
	public Result loginUser(@Param("redirectTo") String redirectLink, @Param("publicAppId") String publicAppId,
			@Param("privateAppId") String privateAppId, @Param("noRedirect") Boolean noRedirect, UserDto user) {
		log.info("loginUser called email : " + user.getEmail());
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			userFacade.verifyUser(user);
			
			Long expiryDuration = 600000l;
			UserDto token = tokenFacade.createUserToken(user.getEmail(), expiryDuration);
			if (noRedirect != null && noRedirect == true) {
				return Results.ok().json().render(token);
			}
			redirectLink += "/" + user.getEmail() + "/" + token.getToken();
			return Results.redirect(redirectLink);
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidUserException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (UUSEncryptionException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (UserNotActiveException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.badRequest().json().render("Could not login user. Please try again"));
		}
	}

	@Override
	public Result forgotPassword(@Param("email") String email, @Param("redirectLink") String redirectLink,
			@Param("publicAppId") String publicAppId, @Param("privateAppId") String privateAppId,
			@Param("noRedirect") Boolean noRedirect) {
		log.info("forgotPassword called email : " + email);

		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			userFacade.isValidForgetPasswordRequest(email);
			userFacade.sendResetPasswordLink(email, publicAppId, redirectLink);
			return controllerUtils.resultWithCorsHeaders(
					Results.ok().json().render("Reset password link is sent to your registered email."));

		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidUserException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("User Does Not Exist"));
		} catch (Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		}
	}

	@Override
	public Result resetPassword(UserDto user, @Param("email") String email, @Param("redirectLink") String redirectLink,
			@Param("publicAppId") String publicAppId, @Param("validationHash") String validationHash,
			@Param("noRedirect") Boolean noRedirect) {
		log.info("resetPassword called email : " + email);
		try {
			appFacade.checkRequestedAppValidity(publicAppId, null, noRedirect);
			userFacade.isValidPasswordResetRequest(user.getEmail(), publicAppId, validationHash);
			userFacade.resetNewPassword(user);
			Long expiryDuration = 600000l;
			if (noRedirect != null && noRedirect == true) {
				UserDto token = tokenFacade.createUserToken(user.getEmail(), expiryDuration);
				return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(token));
			}
			return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(redirectLink));

		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (InvalidResetPasswordHash e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_HASH).json().render(e.getMessage()));
		} catch (UUSException | UUSEncryptionException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch (UserNotActiveException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
	}

	@Override
	public Result resetEmail(@Param("comment") String comment, @Param("email") String email,
			@Param("newEmail") String newEmail, Context context) {
		log.info("resetEmail called from :" + email + "|| To :" + newEmail);
		log.info(context.getSession().get("username") + "reset");
		if (context.getSession().get("username") != null) {
			long id = -1;
			newEmail = newEmail.toLowerCase();
			email = email.toLowerCase();
			long startTime = System.currentTimeMillis();
			try {
				userFacade.isValidEmailChangeRequest(email, newEmail);
				id = userFacade.setEmailStatus(email, newEmail, true, EmailUpdateStatus.PROCESSING, comment);
				return updateEmailInAllSystems(comment, email, newEmail, id, startTime);
			} catch (UUSException e) {
				userFacade.FinalUpdateEmailStatus(id, email, newEmail, comment, startTime);
				log.info("reverted " + id);
				e.printStackTrace();
				return controllerUtils.resultWithCorsHeaders(
						Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
			} catch (InvalidEmailException e) {
				try {
					id = userFacade.setEmailStatus(email, newEmail, false, EmailUpdateStatus.FAILED, comment);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				return controllerUtils.resultWithCorsHeaders(
						Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
			} catch (Exception e) {
				userFacade.FinalUpdateEmailStatus(id, email, newEmail, comment, startTime);
				e.printStackTrace();
				return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
			}
		} else {
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized());
		}
	}

	private Result updateEmailInAllSystems(String comment, String email, String newEmail, long trackedId, long startTime) {
		log.info("updateEmailInAllSystems called with email : "+email+" | newEmail : "+newEmail);
		try {
			updateEmailInMapit(email, newEmail, trackedId);
			updateEmailInMatch(comment, email, newEmail, trackedId, startTime);
			finalUpdateEmailInUUSNEmailChangeTracker(email, newEmail, trackedId);
		} catch (UUSException e) {
			log.info("reverting updates");
			userFacade.FinalUpdateEmailStatus(trackedId, email, newEmail, comment, startTime);
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		} catch (Exception e) {
			log.info("reverting updates");
			userFacade.FinalUpdateEmailStatus(trackedId, email, newEmail, comment, startTime);
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
		tokenFacade.flushTokensByEmailId(newEmail);
		return controllerUtils.resultWithCorsHeaders(Results.ok().json().render("Successfully Updated"));
	}
	
	private void finalUpdateEmailInUUSNEmailChangeTracker(String email, String newEmail, long trackedId)
			throws UUSException {
		log.info("Updating email in UUS");
		updateEmailInUUS(email, newEmail, trackedId);
		sendResetPasswordEmail(newEmail);
		userFacade.setStatus(trackedId, EmailUpdateStatus.DONE);
	}

	private void updateEmailInMatch(String comment, String email, String newEmail, long trackedId, long startTime) {
		log.info("Updating email in Match called with email : "+email +" | newEmail : "+newEmail);
		UusResponseDto matchEmailChangeResponse = userFacade.updateInMatch(email, newEmail);
		boolean isMatchSuccess = matchEmailChangeResponse.isSuccess();
		if (!isMatchSuccess) {
			// reverting in mapit
			userFacade.updateEmailTrackingStatus(trackedId, Servers.MATCH, EmailUpdateStatus.FAILED,
					matchEmailChangeResponse);
			userFacade.FinalUpdateEmailStatus(trackedId, email, newEmail, comment, startTime);
			userFacade.setStatus(trackedId, EmailUpdateStatus.FAILED);
			log.info("Error in Updating email in Match with message : "+matchEmailChangeResponse.getError());
			throw new UUSException("Error in updating email from Match : "+matchEmailChangeResponse.getError());
		} else {
			log.debug("Success : email is updated in Match");
		}
		userFacade.updateEmailTrackingStatus(trackedId, Servers.MATCH, EmailUpdateStatus.DONE,
				matchEmailChangeResponse);
	}

	private void updateEmailInMapit(String email, String newEmail, long trackedId) {
		log.info("Updating email in Mapit is called with email : "+email+" | newEmail : "+newEmail);
		UusResponseDto mapitEmailChangeResponse = userFacade.updateInMapit(email, newEmail);
		boolean isMapitSuccess = mapitEmailChangeResponse.isSuccess();
		if (!isMapitSuccess) {
			log.info("Error in Updating email in Mapit with message : "+mapitEmailChangeResponse.getError());
			userFacade.updateEmailTrackingStatus(trackedId, Servers.MAPIT, EmailUpdateStatus.FAILED,
					mapitEmailChangeResponse);
			userFacade.setStatus(trackedId, EmailUpdateStatus.FAILED);
			throw new UUSException("Error in updating email from Mapit : "+mapitEmailChangeResponse.getError());
		} else {
			log.debug("Success : email is updated in mapit");
		}
		userFacade.updateEmailTrackingStatus(trackedId, Servers.MAPIT, EmailUpdateStatus.DONE,
				mapitEmailChangeResponse);
	}

	private void sendResetPasswordEmail(String newEmail) throws UUSException {
		log.info("sendResetPasswordEmail called with newEmail:"+newEmail);
		try {
		String publicAppId = userFacade.getMapitPublicAppId();
		String redirectLink = "https://match.myanatomy.in";
		userFacade.sendResetPasswordLink(newEmail, publicAppId, redirectLink);
		}catch(UUSEncryptionException e){
			throw new UUSException("Error in Match");
		}
		
	}

	private void updateEmailInUUS(String email, String newEmail, long id) {
		log.info("updateEmailInUUS called with email : "+email+" | newEmail :"+newEmail);
		UusResponseDto uusResponse = userFacade.updateEmailInUus(email, newEmail);
		userFacade.updateEmailTrackingStatus(id, Servers.UUS, EmailUpdateStatus.DONE, uusResponse);
	}

	@Override
	public Result updatePassword(UserDto userDto, @Param("publicAppId") String publicAppId,
			@Param("privateAppId") String privateAppId, @Param("noRedirect") boolean noRedirect) {
		log.info("updatePassword called");
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			if (privateAppId == null) {
				userFacade.updatePassword(userDto, publicAppId);
				return controllerUtils.resultWithCorsHeaders(Results.ok().json().render("true"));
			}
			userFacade.updatePasswordByAdmin(userDto, publicAppId);
			return Results.ok().json().render("true");
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UUSException | UUSEncryptionException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (Exception e) {
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
	}

	@Override
	public Result logout(@Param("uusToken") String uusToken, @Param("removeAllToken") Boolean removeAllToken) {
		log.info("logout called removeAllToken : " + removeAllToken);
		try {
			tokenFacade.removeToken(uusToken, removeAllToken);
			return controllerUtils.resultWithCorsHeaders(Results.ok().json().render("Logged out successfully"));
		} catch (UUSException e) {
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		}
	}

	@Override
	public Result verifyToken(@Param("token") String uusToken, @Param("privateAppId") String privateAppId,
			@Param("publicAppId") String publicAppId, @Param("noRedirect") boolean noRedirect) {
		log.info("verifyToken called");
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			if (!tokenFacade.isValidToken(uusToken)) {
				return Results.unauthorized().json().render("Invalid token");
			}
			return Results.ok().json().render(tokenFacade.getUserDtoByUusToken(uusToken));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UUSException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
	}

	@Override
	public Result inviteNRegister(@Param("publicAppId") String publicAppId, @Param("privateAppId") String privateAppId,
			@Param("noRedirect") Boolean noRedirect, @Param("redirectLink") String redirectLink, List email) {
		log.info("inviteNRegister called");
		List<String> emails = new ArrayList<>(email);
		if (!appFacade.isValidPublicOrPrivateAppId(publicAppId, privateAppId, noRedirect)) {
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json()
					.render("Invalid public or private app id provided");
		}
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			return Results.ok().json()
					.render(userFacade.inviteCandidate(emails, publicAppId, redirectLink, privateAppId));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return Results.badRequest().json()
					.render("An exception has occurred due to some bad request.Please Try Again");
		}
	}

	@Override
	public Result getSecuredTokenByEmail(@Param("email") String email, @Param("privateAppId") String privateAppId,
			@Param("publicAppId") String publicAppId, @Param("noRedirect") boolean noRedirect) {
		log.info("getSecuredTokenByEmail email : " + email);
		if (!appFacade.isValidPublicOrPrivateAppId(publicAppId, privateAppId, noRedirect)) {
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json()
					.render("Invalid public or private app id provided");
		}
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			Long expiryDuration = 600000l;
			UserDto token = tokenFacade.createUserToken(email, expiryDuration);
			return Results.ok().json().render(token);
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UUSException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch (UserNotActiveException e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return Results.badRequest().json()
					.render("An exception has occurred due to some bad request.Please Try Again");
		}
	}

	@Override
	public Result checkCandidateExistence(@Param("publicAppId") String publicAppId,
			@Param("privateAppId") String privateAppId, List emails) {
		log.info("checkCandidateExistence called");
		List<String> emailsList = new ArrayList<>(emails);
		if (!appFacade.isValidPublicOrPrivateAppId(publicAppId, privateAppId, true)) {
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json()
					.render("Invalid public or private app id provided");
		}
		try {
			userFacade.userExistAsCandidate(emailsList);
			return Results.ok().json().render("An exception has occurred due to some bad request.Please Try Again");
		} catch (Exception e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.CANDIDATE_EXIST).json().render("Email Exist As Candidate");
		}
	}

	public Result checkUserExist(@Param("publicAppId") String publicAppId, @Param("privateAppId") String privateAppId,
			@Param("email") String email) {
		log.info("checkUserExist Called");
		if (!appFacade.isValidPublicOrPrivateAppId(publicAppId, privateAppId, true)) {
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json()
					.render("Invalid public or private app id provided");
		}
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, true);

			boolean userExist = userFacade.isUserAlreadyExists(email);
			return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(userExist));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_REQUEST).json()
					.render("An exception has occurred due to some bad request.Please Try Again"));
		}
	}

	@Override
	public Result registerUsersInBulk(@Param("publicAppId") String publicAppId,
			@Param("privateAppId") String privateAppId, @Param("redirectLink") String redirectLink,
			@Param("noRedirect") boolean noRedirect, BulkUserDto bulkList) {
		log.info("registerUsersInBulk called");
		if (!appFacade.isValidPublicOrPrivateAppId(publicAppId, privateAppId, noRedirect)) {
			return Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json()
					.render("Invalid public or private app id provided");
		}
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			return Results.ok().json().render(userFacade.createUsersInBulk(bulkList.getUsersDto(), publicAppId, privateAppId, redirectLink));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (Exception e) {
			log.info("Error :" + e);
			e.printStackTrace();
			return Results.badRequest().json().render(e);
		}
	}
	
	@Override
	public Result loginSubUser(@Param("redirectTo") String redirectLink, @Param("noRedirect") boolean noRedirect, @Param("publicAppId") String publicAppId,
			@Param("privateAppId") String privateAppId, @Param("uusToken") String uusToken, @Param("subUserUuid") Long subUserUuid) {
		log.info("loginSubUser is called with subUserUuid : "+subUserUuid);
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			if (!tokenFacade.isValidToken(uusToken)) {
				return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Invalid token"));
			}
			String encryptedToken = userFacade.getSubUserToken(uusToken, subUserUuid);
			if (noRedirect == true) {
				return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(encryptedToken));
			}
			redirectLink += "/" + encryptedToken;
			return controllerUtils.resultWithCorsHeaders(Results.redirect(redirectLink));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UUSException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch(Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
	}
	
	@Override
	public Result validateSubUserActionWithToken(@Param("publicAppId") String publicAppId, @Param("privateAppId") String privateAppId, 
			@Param("noRedirect") boolean noRedirect, @Param("subUserToken") String subUserToken,
			@Param("accessArea") String accessArea, @Param("action") String action) {
		log.info("validateSubUserToken is called ");
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			boolean isValidSubUserAction = userFacade.validateSubUserActionWithToken(subUserToken, accessArea, action);
			return controllerUtils
					.resultWithCorsHeaders(Results.ok().json().render(isValidSubUserAction));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UUSException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch (InvalidAccessArea e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch (InvalidAccessPermission e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch (InvalidUusTokenType e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch (AclNotFoundException e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch(Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
	}
	
	@Override
	public Result refreshSubUserToken(@Param("publicAppId") String publicAppId, @Param("privateAppId") String privateAppId, 
			@Param("noRedirect") boolean noRedirect, @Param("subUserToken") String subUserToken) {
		log.info("validateSubUserToken is called ");
		try {
			appFacade.checkRequestedAppValidity(publicAppId, privateAppId, noRedirect);
			String refreshedSubUserToken = userFacade.refreshSubUserToken(subUserToken);
			return controllerUtils
					.resultWithCorsHeaders(Results.ok().json().render(refreshedSubUserToken));
		} catch (UusInvalidPublicAppId e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(
					Results.status(ResponseStatus.INVALID_PUBLIC_APP_ID).json().render(e.getMessage()));
		} catch (UusInvalidPublicOrPrivateAppId e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_APP_ID).json().render(e.getMessage()));
		} catch (UUSException e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch (AclNotFoundException e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render(e.getMessage()));
		} catch (InvalidUusTokenType e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch(Exception e) {
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e.getMessage()));
		}
	}
}
