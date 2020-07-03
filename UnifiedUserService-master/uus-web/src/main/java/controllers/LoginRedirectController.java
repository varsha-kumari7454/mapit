package controllers;

import java.util.List;

import dto.BulkUserDto;
import dto.UserDto;
import ninja.Context;
import ninja.Result;

public interface LoginRedirectController {
	
	Result registerUser(UserDto user, String redirectTo,String publicAppId,String privateAppId,Boolean noRedirect,Boolean noMail,Boolean isReqByAdmin);
	
	Result registerNActivateUser(UserDto user,String publicAppId,String privateAppId,boolean noRedirect);
	
	Result adminRegisteringUserWithoutPassword(UserDto user,String redirectTo,String privateAppId,Boolean noRedirect,String publicAppId, Boolean sendMail);
	
	Result activateUser(String activationCodeHash,String publicAppId,String privateAppId,String email,String redirectLink,Boolean noRedirect);
	
	Result loginUser(String redirectLink,String publicAppId,String privateAppId,Boolean noRedirect, UserDto user);
	
	Result forgotPassword(String email,String redirectLink,String publicAppId,String privateAppId,Boolean noRedirect);
	
	Result resetPassword(UserDto user,String email,String redirectLink,String publicAppId,String validationHash,Boolean noRedirect);
	
	Result updatePassword(UserDto userDto,String publicAppId,String privateAppId,boolean noRedirect);

	Result logout(String uusToken,Boolean removeAllToken);
	
	Result verifyToken(String uusToken,String privateAppId,String publicAppId,boolean noRedirect);

	Result inviteNRegister(String publicAppId,String privateAppId,Boolean noRedirect,String redirectLink, List email);
	
	Result getSecuredTokenByEmail(String email,String privateAppId,String publicAppId,boolean noRedirect);
	
	Result checkCandidateExistence(String publicAppId,String privateAppId,List emails);
	
	Result checkUserExist(String publicAppId,String privateAppId,String email);
	
	Result registerUsersInBulk(String publicAppId,String privateAppId,String redirectLink,boolean noRedirect,BulkUserDto bulkList);

	Result resetEmail(String comment, String email, String newEmail, Context context);

	Result loginSubUser(String redirectLink, boolean noRedirect, String publicAppId, String privateAppId,
			String uusToken, Long subUserUuid);

	Result validateSubUserActionWithToken(String publicAppId, String privateAppId, boolean noRedirect,
			String subUserToken, String accessArea, String action);

	Result refreshSubUserToken(String publicAppId, String privateAppId, boolean noRedirect, String subUserToken);
}
