package controllers;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

import dto.UserDto;
import dto.acl.AccessControlListDto;
import dto.acl.AccessControlListWebDto;
import dto.acl.AccessControlUnitDto;
import dto.acl.AccessControlUnitWebDto;
import dto.acl.MinimalACLWebDto;
import exceptions.AclNotFoundException;
import exceptions.InvalidACLRequest;
import exceptions.InvalidAccessArea;
import exceptions.InvalidAccessPermission;
import exceptions.InvalidEmailException;
import exceptions.UUSEncryptionException;
import exceptions.UusSubUserNotFound;
import exceptions.UusSuperUserNotFound;
import facade.ACLFacade;
import facade.TokenFacade;
import facade.UserFacade;
import models.UserRole;
import ninja.Result;
import ninja.Results;
import ninja.params.Param;

public class AccessControlListControllerImpl implements AccessControlListController {
	private static final Logger log = LogManager.getLogger(AccessControlListControllerImpl.class.getName());
	
	@Inject
	private ACLFacade aclFacade;
	@Inject
	private TokenFacade tokenFacade;
	@Inject
	private UserFacade userFacade;
	@Inject
	private ControllerUtils controllerUtils;
	
	@Override
	public Result getACLByOwner(@Param("uusToken") String uusToken) {
		log.info("getACLByOwner is called");
		try {
			boolean isGetByOwner = true;
			boolean isValidToken = tokenFacade.isValidToken(uusToken);
			if (isValidToken) {
				UserDto userDto = tokenFacade.getUserDtoByUusToken(uusToken);
				if(!userDto.getRole().equals(UserRole.CANDIDATE.toString())) {
					return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(aclFacade.getACL(userDto, isGetByOwner)));
				} else {
					log.error("Error : User is Candidate");
					return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Trying to get Sub Users for Candidate."));
				}
			}
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Invalid Token"));
		} catch(UusSuperUserNotFound e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Super User/Owner not found : "+e.getMessage()));
		} catch(Exception e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("An error Occured : "+e.getMessage()));
		}
	}
	
	@Override
	public Result updateACL(@Param("uusToken") String uusToken, MinimalACLWebDto minimalAclDto) {
		//update individual subuser data list 
		log.info("updateACL is called");
		try {
			boolean isValidToken = tokenFacade.isValidToken(uusToken);
			if (isValidToken) {
				
				Long subUserUuid = minimalAclDto.getSubUserId();
				UserDto userDto = tokenFacade.getUserDtoByUusToken(uusToken);
				boolean isValidSubUserId = userFacade.isValidSubUserId(subUserUuid);
				
				if(!isValidSubUserId) {
					return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Sub User is Candidate"));
				}
				
				if(!userDto.getRole().equals(UserRole.CANDIDATE.toString())) {
					aclFacade.updateACL(userDto, minimalAclDto);
					return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(getAclWebDtoForUserDto(userDto)));
				} else {
					log.error("Error : User is Candidate");
					return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Cannot Add/Update Subusers for Candidate."));
				}
			}
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Invalid Token"));
		} catch(UusSuperUserNotFound e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Super User/Owner not found : "+e.getMessage()));
		} catch(UusSubUserNotFound e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Sub User not found : "+e.getMessage()));
		} catch(AclNotFoundException e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.ACL_DOES_NOT_EXIST).json().render(e.getMessage()));
		} catch (InvalidAccessArea e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch (InvalidAccessPermission e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch (InvalidACLRequest e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch(Exception e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("An error Occured : "+e.getMessage()));
		}
	}
	
	@Override
	public Result addACL(@Param("uusToken") String uusToken, @Param("subUserEmail") String subUserEmail, @Param("subUserName") String subUserName, AccessControlUnitWebDto aclUnitWebDto) {
		//add individual subuser data list 
		subUserEmail = subUserEmail.toLowerCase();
		log.info("addACL is called with subUserEmail : "+subUserEmail);
		try {
			boolean isValidToken = tokenFacade.isValidToken(uusToken);
			if (isValidToken) {
				UserDto userDto = tokenFacade.getUserDtoByUusToken(uusToken);
				if(!userDto.getRole().equals(UserRole.CANDIDATE.toString())) {
					boolean isCandidate = createSubUserIfNotExists(subUserEmail, subUserName);
					if(isCandidate) {
						return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Sub User Email is Candidate"));
					}
					aclFacade.addACL(userDto, subUserEmail, aclUnitWebDto);
					return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(getAclWebDtoForUserDto(userDto)));
				} else {
					log.error("Error : User is Candidate");
					return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Cannot Add/Update Subusers for Candidate."));
				}
			}
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Invalid Token"));
		} catch(UusSuperUserNotFound e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Super User/Owner not found : "+e.getMessage()));
		} catch(UusSubUserNotFound e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Sub User not found : "+e.getMessage()));
		} catch(UUSEncryptionException e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("Error occured while creating the Sub User "+e.getMessage()));
		} catch(InvalidEmailException e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_EMAIL).json().render("Not a valid email to create Sub User "+e.getMessage()));
		} catch (InvalidAccessArea e) {
			e.printStackTrace();
			return controllerUtils
					.resultWithCorsHeaders(Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage()));
		} catch (InvalidAccessPermission e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch (InvalidACLRequest e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch(Exception e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("An error Occured : "+e.getMessage()));
		}
	}
	
	private boolean createSubUserIfNotExists(String subUserEmail, String subUserName) throws UUSEncryptionException {
		log.info("createSubUserIfNotExists is Called with subUserEmail : "+ subUserEmail + " | subUserName : "+subUserName);
		boolean isSubUserCandidate = false;
		boolean isSubUserExist = userFacade.isSubUserExistByEmail(subUserEmail);
		if(isSubUserExist) {	
			isSubUserCandidate = isSubUserEmailHr(subUserEmail, isSubUserCandidate);
		} else {
			userFacade.createSubUserWithMinimalDetailsByEmail(subUserEmail, subUserName);
		}
		return isSubUserCandidate;
	}

	private boolean isSubUserEmailHr(String subUserEmail, boolean isSubUserCandidate) {
		log.info("isSubUserEmailHr is called with subUserEmail : "+subUserEmail);
		boolean isSubUserEmailHr = userFacade.isValidSubUserEmail(subUserEmail);
		if(!isSubUserEmailHr) {
			isSubUserCandidate = true;
		}
		return isSubUserCandidate;
	}
	
	@Override
	public Result deleteACL(@Param("uusToken") String uusToken, MinimalACLWebDto minimalAclDto) {
		//delete individual subuser data list
		log.info("deleteACL is called");
		try {
			boolean isValidToken = tokenFacade.isValidToken(uusToken);
			if (isValidToken) {
				
				Long subUserUuid = minimalAclDto.getSubUserId();
				UserDto userDto = tokenFacade.getUserDtoByUusToken(uusToken);
				boolean isValidSubUserId = userFacade.isValidSubUserId(subUserUuid);
				
				if(!isValidSubUserId) {
					return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Sub User is Candidate"));
				}
				
				if(!userDto.getRole().equals(UserRole.CANDIDATE.toString())) {
					aclFacade.deleteACL(userDto, minimalAclDto);
					return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(getAclWebDtoForUserDto(userDto)));
				} else {
					log.error("Error : User is Candidate");
					return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Trying to delete Subusers for Candidate."));
				}
			}
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Invalid Token"));
		} catch(UusSuperUserNotFound e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Super User/Owner not found : "+e.getMessage()));
		} catch(AclNotFoundException e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.ACL_DOES_NOT_EXIST).json().render(e.getMessage()));
		} catch (InvalidACLRequest e) {
			e.printStackTrace();
			return Results.status(ResponseStatus.INVALID_REQUEST).json().render(e.getMessage());
		} catch(Exception e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("An error Occured : "+e.getMessage()));
		}
	}

	@Override
	public Result getACLBySubUser(@Param("uusToken") String uusToken) {
		log.info("getACLBySubUser is called");
		try {
			boolean isValidToken = tokenFacade.isValidToken(uusToken);
			boolean isGetByOwner = false;
			if (isValidToken) {
				UserDto userDto = tokenFacade.getUserDtoByUusToken(uusToken);
				if(!userDto.getRole().equals(UserRole.CANDIDATE.toString())) {
					return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(aclFacade.getACL(userDto, isGetByOwner)));
				} else {
					log.error("Error : User is Candidate");
					return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Trying to get Sub Users for Candidate."));
				}
			}
			return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Invalid Token"));
		} catch(UusSuperUserNotFound e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.status(ResponseStatus.USER_DOES_NOT_EXIST).json().render("Super User/Owner not found : "+e.getMessage()));
		} catch(Exception e) {
			log.error("Error : "+e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("An error Occured : "+e.getMessage()));
		}
	}
	
	private AccessControlListWebDto getAclWebDtoForUserDto(UserDto userDto) {
		log.info("getAclWebDtoForUserDto is called");
		return aclFacade.getAclWebDtoByOwnerUserDto(userDto);
	}
}
