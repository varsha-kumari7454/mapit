package facade;

import java.security.acl.AclNotFoundException;
import java.util.List;

import dto.BulkRegisterResponseDto;
import dto.UserDto;
import dto.UusResponseDto;
import exceptions.CandidateAlreadyExist;
import exceptions.InvalidAccessArea;
import exceptions.InvalidAccessPermission;
import exceptions.InvalidActivationHash;
import exceptions.InvalidEmailFormatException;
import exceptions.InvalidResetPasswordHash;
import exceptions.InvalidUusTokenType;
import exceptions.UUSEncryptionException;
import exceptions.UUSException;
import exceptions.UusInvalidUserException;
import exceptions.InvalidEmailException;
import models.EmailUpdateStatus;
import models.RegisteredApp;
import models.RegisteredAppUser;
import models.Servers;

public interface UserFacade {

	void adminRegisteringUser(UserDto user, String redirectTo, String privateAppId, Boolean sendMail) throws UUSEncryptionException;

	String registerUser(UserDto user, String publicAppId, String redirectTo, String privateAppId, Boolean noMail, Boolean isReqByAdmin)
			throws UUSEncryptionException;

	void verifyUser(UserDto user) throws UUSEncryptionException;

	void isValidUserActivationHash(String activationCodeHash, String publicAppId, String email)throws UUSEncryptionException,InvalidActivationHash;

	void updateUserActivationStatus(String email, boolean isActiveUser);

	RegisteredAppUser getRegisteredUserByEmail(String email);

	void isValidRegisterRequest(String email);

	boolean isValidAdminRegisterRequest(String publicAppId, String redirectTo, UserDto user, String privateAppId);

	boolean isUserAlreadyExists(String email);

	void sendResetPasswordLink(String email, String publicAppId, String redirectLink) throws UUSEncryptionException;

	void isValidPasswordResetRequest(String email, String publicAppId, String validationHash) throws UUSEncryptionException,InvalidResetPasswordHash;
	
	long setEmailStatus(String email, String newEmail, boolean b, EmailUpdateStatus emailUpdateStatus, String comment) throws Exception;
	
	UusResponseDto updateEmailInUus(String email, String newEmail) throws UUSException;
	
	void resetNewPassword(UserDto user) throws UUSEncryptionException;

	BulkRegisterResponseDto inviteCandidate(List<String> emails, String publicAppId, String redirectLink,
			String privateAppId) throws Exception;

	void updatePassword(UserDto userDtoWithOldPassword, String publicAppId2) throws UUSEncryptionException;

	List<RegisteredApp> getRegisteredAppByPrivateAppId(String privateAppId);

	RegisteredAppUser createUserWithoutPassword(UserDto user, List<RegisteredApp> registeredApps, boolean isCreatedAsSubUser)
			throws UUSEncryptionException;

	void userExistAsCandidate(List<String> emails) throws CandidateAlreadyExist;

	void createUser(UserDto user, String publicAppId, String privateAppId) throws Exception;

	UserDto isUserAlreadyExistsWithDetails(String email);

	boolean getIntermediateUser(String email, String password);

	void updatePasswordByAdmin(UserDto userDto, String publicAppId) throws UUSEncryptionException;

	BulkRegisterResponseDto createUsersInBulk(List<UserDto> list, String publicAppId, String privateAppId,
			String redirectLink) throws Exception;
	
	void isValidForgetPasswordRequest(String email) throws UUSException, UusInvalidUserException;

	void updateEmailTrackingStatus(long id, Servers server, EmailUpdateStatus emailUpdateStatus, UusResponseDto uusResponseDto);

	String getMapitEmailUpdateUrl(String email, String newEmail);

	void setStatus(long id, EmailUpdateStatus status);

	UusResponseDto updateInMapit(String email, String newEmail);

	void FinalUpdateEmailStatus(long id, String email, String newEmail, String comment, long startTime);

	UusResponseDto updateInMatch(String email, String newEmail);

	String getMatchEmailUpdateUrl(String email, String newEmail);

	boolean isValidEmailChangeRequest(String email, String newEmail);

	void validateFromEmailId(String fromEmail);

	void validateToEmail(String toEmail);

	boolean isRegisterUserExistByEmail(String email) throws InvalidEmailException;	
	
	String getMapitPublicAppId();

	boolean isValidSubUserEmail(String email);

	boolean isValidSubUserId(Long uuid);

	RegisteredAppUser getRegisteredAppUserByUuid(Long uuid);

	boolean isSubUserExistByEmail(String email);

	void createSubUserWithMinimalDetailsByEmail(String email, String userName) throws UUSEncryptionException;

	List<RegisteredApp> getRegisteredAppsByName(String name);

	String getSubUserToken(String ownerUusToken, Long subUserUuid);

	boolean validateSubUserActionWithToken(String subUserToken, String accessArea, String action) throws InvalidAccessArea, InvalidAccessPermission, InvalidUusTokenType;

	String refreshSubUserToken(String subUserToken) throws InvalidUusTokenType, AclNotFoundException;
}
