package dto;

import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Strings;
import com.google.inject.Inject;

import dto.acl.AccessAreaDto;
import dto.acl.AccessControlListDto;
import dto.acl.AccessControlUnitDto;
import dto.acl.AccessControlUnitWebDto;
import dto.acl.AccessPermissionDto;
import dto.acl.MinimalACLWebDto;
import dto.acl.SubUserDetailsDto;
import dto.acl.SubUserLogInDto;
import exceptions.UUSEncryptionException;
import exceptions.UUSException;
import models.AccessArea;
import models.AccessControlList;
import models.AccessControlUnitEntityDto;
import models.AccessPermission;
import models.IndividualAccessControlListEntityDto;
import models.RegisteredApp;
import models.RegisteredAppUser;
import ninja.utils.NinjaProperties;

public class DtoUtils {

	public static final String permanantSalt = "jhberib4387rwhubdfsiuweiuwakjbq3wHUF@!DFGEDW@%IHDJBKSU#983nsziu82w!";
	public static final String HMAC_SHA256 = "HmacSHA256";
	public static final String regex = "^([.a-zA-Z0-9_-]+)@([a-zA-Z0-9_-]+[.])+([a-zA-Z]{2,15})$";
	private static final Logger log = LogManager.getLogger(DtoUtils.class);

	@Inject
	NinjaProperties ninjaProperties;

	public static String encrypt(String subject) throws UUSEncryptionException {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			byte[] passBytes = subject.getBytes();
			byte[] passHash = sha256.digest(passBytes);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < passHash.length; i++) {
				sb.append(Integer.toString((passHash[i] & 0xff) + 0x100, 16).substring(1));
			}
			String generatedPassword = sb.toString();
			return generatedPassword;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new UUSEncryptionException("Couldn't encrypt the subject");
		}
	}

	public static String generateStringFromTemplate(String template, Map<String, String> scope) {
		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache m = mf.compile(template);
		StringWriter sw = new StringWriter();
		m.execute(sw, scope);
		return sw.toString();
	}

	public static RegisteredAppDto mapToDto(RegisteredApp registeredApp) {
		RegisteredAppDto ans = new RegisteredAppDto();
		ans.setAppSecret(registeredApp.getAppSecret());
		ans.setLogoURL(registeredApp.getLogoURL());
		ans.setName(registeredApp.getName());
		ans.setPublicAppId(registeredApp.getPublicAppId());
		ans.setUUID(registeredApp.getUUID());
		return ans;
	}

	public static boolean isStringEmptyOrNull(String keyString) {
		if (Strings.isNullOrEmpty(keyString)) {
			return true;
		}
		return false;
	}

	public static String getAppNameAsString(RegisteredAppUser registeredAppUser) {
		String appNameList = "$";
		for (RegisteredApp registeredApp : registeredAppUser.getRegisteredApps()) {
			appNameList += registeredApp.getName() + "$";
		}
		return appNameList;
	}

	public static String getUserToken(TokenDto tokenDto, String signiture) {
		Algorithm algorithm = Algorithm.HMAC256(signiture);
		String token = JWT.create().withIssuer("uus").withClaim(TokenNames.userName.toString(), tokenDto.getEmail())
				.withClaim(TokenNames.app.toString(), tokenDto.getAppNameList())
				.withClaim(TokenNames.role.toString(), tokenDto.getUserRole())
				.withClaim(TokenNames.uuid.toString(), tokenDto.getUuid())
				.withClaim(TokenNames.tid.toString(), tokenDto.getTokenId())
				.withClaim(TokenNames.exp.toString(), tokenDto.getExpiryDate())
				.withClaim(TokenNames.createdAsSubRole.toString(), tokenDto.isCreatedAsSubRole())
				.withClaim(TokenNames.hasSuperUser.toString(), tokenDto.isHasSuperUser()).sign(algorithm);
		return token;
	}

	public static TokenDto extractToken(String token, String signiture) throws UUSException {
		TokenDto ans = new TokenDto();
		Algorithm algorithm = Algorithm.HMAC256(signiture);
		
		try {
			
			JWTVerifier verifier = JWT.require(algorithm).withIssuer("uus").build(); // Reusable
																						// verifier
																						// instance
			DecodedJWT jwt = verifier.verify(token);
			ans.setEmail(jwt.getClaim(TokenNames.userName.toString()).asString());
			ans.setAppNameList(jwt.getClaim(TokenNames.app.toString()).asString());
			ans.setUserRole(jwt.getClaim(TokenNames.role.toString()).asString());
			ans.setUuid(jwt.getClaim(TokenNames.uuid.toString()).asLong());
			ans.setTokenId(jwt.getClaim(TokenNames.tid.toString()).asLong());
			ans.setExpiryDate(jwt.getClaim(TokenNames.exp.toString()).asLong());
			ans.setCreatedAsSubRole(jwt.getClaim(TokenNames.createdAsSubRole.toString()).asBoolean());
			ans.setHasSuperUser(jwt.getClaim(TokenNames.hasSuperUser.toString()).asBoolean());
			ans.setToken(token);
	
			return ans;
		} catch (Exception e) {
			log.info("Error: ", e);
			throw new UUSException("Unable to Extract Token");
		}
	}

	// TODO reset password should have a parameter that changes so that the
	// reset password hash is not used multiple times
	public static String getPasswordResetHash(String email, String publicAppId, Long uuid, String oldPassword)
			throws UUSEncryptionException {
		return getHash(email + publicAppId + uuid + oldPassword);
	}

	public static String getHash(String key) throws UUSEncryptionException {
		return DtoUtils.encrypt(key + permanantSalt);
	}

	public static String getUserActivationHash(String email, Long uuid, String publicAppId)
			throws UUSEncryptionException {
		return DtoUtils.encrypt(email + publicAppId + uuid);
	}

	public static String getPasswordHash(String email, String password) throws UUSEncryptionException {
		return DtoUtils.getHash(email + password);
	}

	public static String generateMatchPasswordHash(String password, String salt) {
		try {
			final Mac hMacSHA256 = Mac.getInstance(HMAC_SHA256);
			byte[] hmacKeyBytes = salt.getBytes();
			final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, HMAC_SHA256);
			hMacSHA256.init(secretKey);
			byte[] dataBytes = password.getBytes();
			byte[] digested = hMacSHA256.doFinal(dataBytes);
			StringBuilder sb = new StringBuilder();
			for (byte b : digested) {
				sb.append(String.format("%02X", b));
			}
			return sb.toString().toLowerCase();
		} catch (Exception e) {
			log.debug("Error in Password");
			return null;
		}
	}

	public static String encryptMd5(String password) {
		log.debug("encrypt called");
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] passBytes = password.getBytes();
			md.reset();
			byte[] digested = md.digest(passBytes);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < digested.length; i++) {
				sb.append(Integer.toHexString(0xff & digested[i]));
			}
			log.debug("returning encrypted string");
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			log.debug("Exception thrown in encrypt  :" + e);
			return null;
		}
	}

	public static String randomString(int length) {
		return RandomStringUtils.random(length,
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray());
	}

	public static boolean isEmailValid(String email) {
		if(email==null){
			return false;
		}
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}
	
	public static RegisteredAppDto mapToRegisteredAppDto(RegisteredApp registeredApp){
		RegisteredAppDto registeredAppDto = new RegisteredAppDto();
		registeredAppDto.setLogoURL(registeredApp.getLogoURL());
		registeredAppDto.setAppSecret(registeredApp.getAppSecret());
		registeredAppDto.setName(registeredApp.getName());
		registeredAppDto.setPublicAppId(registeredApp.getPublicAppId());
		registeredAppDto.setUUID(registeredApp.getUUID());
		return registeredAppDto;
	}
	
	public static AccessControlListDto mapAccessControlListToDto(AccessControlList accessControlList) {
		AccessControlListDto accessControlListDto = new AccessControlListDto();
		
		accessControlListDto.setId(accessControlList.getId());
		accessControlListDto.setCreationTime(accessControlList.getCreationTime());
		accessControlListDto.setLastmodifiedTime(accessControlList.getLastmodifiedTime());
		
		List<AccessControlUnitDto> individualAclUnitsDtoList = mapIndividualAclToAccessControlUntiList(accessControlList);
		
		accessControlListDto.setAclUnits(individualAclUnitsDtoList);
		
		return accessControlListDto;
	}

	public static List<AccessControlUnitDto> mapIndividualAclToAccessControlUntiList(AccessControlList accessControlList) {
		List<AccessControlUnitDto> individualAclUnitsDtoList = new ArrayList<>();
		IndividualAccessControlListEntityDto individualACL = accessControlList.getiACL();
		if(individualACL != null) {
			for(AccessControlUnitEntityDto aclUnitEntityDto : individualACL.getAccessControlUnit()) {
				
				AccessControlUnitDto  aclUnitDto = new AccessControlUnitDto();
				aclUnitDto.setArea(mapAccessAreaToDto(aclUnitEntityDto.getArea()));
				
				List<AccessPermissionDto> accessPermissionDtoList = new ArrayList<>();
				List<AccessPermission> sanitizedAccessPermissions = sanitizeAccessPermissions(aclUnitEntityDto.getPermissions()); 
				
				for(AccessPermission accessPermission : sanitizedAccessPermissions) {
					AccessPermissionDto accessPermissionDto = mapAccessPermissionToDto(accessPermission);
					accessPermissionDtoList.add(accessPermissionDto);
				}
				aclUnitDto.setPermissions(accessPermissionDtoList);

				aclUnitDto.setAccessGrantedFrom(aclUnitEntityDto.getAccessGrantedFrom());
				aclUnitDto.setAccessGrantedTill(aclUnitEntityDto.getAccessGrantedTill());
				
				individualAclUnitsDtoList.add(aclUnitDto);
			}
		}
		return individualAclUnitsDtoList;
	}
	
	public static MinimalUserDetailsDto mapRegisteredAppUserToMinimalUserDetails(RegisteredAppUser user) {
		MinimalUserDetailsDto minimalUserDetails = new MinimalUserDetailsDto();
		if(user != null) {
			minimalUserDetails.setUuid(user.getUuid());
			if(user.getUserDetails() != null) {
				String name = user.getUserDetails().getName()==null ? getNameFromEmail(user.getEmail()) : user.getUserDetails().getName(); 
				minimalUserDetails.setName(name);
			}
			minimalUserDetails.setEmail(user.getEmail());
		}
		return minimalUserDetails;
	}
	
	public static MinimalUserDetailsDto mapUserDtoToMinimalUserDetails(UserDto user) {
		MinimalUserDetailsDto minimalUserDetails = new MinimalUserDetailsDto();
		if(user != null) {
			minimalUserDetails.setUuid(user.getUuid());
			String name = user.getName()==null ? getNameFromEmail(user.getEmail()) : user.getName(); 
			minimalUserDetails.setName(name);
			minimalUserDetails.setEmail(user.getEmail());
		}
		return minimalUserDetails;
	}
	
	public static dto.acl.AccessAreaDto mapAccessAreaToDto(models.AccessArea accessArea) {
		dto.acl.AccessAreaDto accessAreaDto = null;
		switch(accessArea) {
			case JOB : 
				accessAreaDto= dto.acl.AccessAreaDto.JOB;
				break;
			case EVENT : 
				accessAreaDto = dto.acl.AccessAreaDto.EVENT;
				break;
			case ANNOUNCEMENT : 
				accessAreaDto = dto.acl.AccessAreaDto.ANNOUNCEMENT;
				break;
			case ATTENDANCE : 
				accessAreaDto = dto.acl.AccessAreaDto.ATTENDANCE;
				break;
			case DOWNLOAD : 
				accessAreaDto = dto.acl.AccessAreaDto.DOWNLOAD;
				break;
			case PROFILE : 
				accessAreaDto = dto.acl.AccessAreaDto.PROFILE;
				break;
		}
		
		return accessAreaDto;
	}
	
	public static dto.acl.AccessPermissionDto mapAccessPermissionToDto(models.AccessPermission accessPermission) {
		dto.acl.AccessPermissionDto accessPermissionDto = null;
		switch(accessPermission) {
			case MODIFY :
				accessPermissionDto = dto.acl.AccessPermissionDto.MODIFY;
				break;
			case ADMIN :
				accessPermissionDto = dto.acl.AccessPermissionDto.ADMIN;
				break;
			case PUBLISH :
				accessPermissionDto = dto.acl.AccessPermissionDto.PUBLISH;
				break;
			case REMOVE :
				accessPermissionDto = dto.acl.AccessPermissionDto.REMOVE;
				break;
			case VIEW :
				accessPermissionDto = dto.acl.AccessPermissionDto.VIEW;
				break;
		}
		
		return accessPermissionDto;
	}
	
	public static AccessControlUnitEntityDto mapAclUnitDtoToEntityDto(AccessControlUnitDto aclUnitDto) {
		
		AccessControlUnitEntityDto aclEntityDto = new AccessControlUnitEntityDto();
		aclEntityDto.setArea(mapAccessAreaDtoToAcessArea(aclUnitDto.getArea()));
		
		List<AccessPermission> accessPermissionsList = new ArrayList<>();
		List<AccessPermissionDto> sanitizedAccessPermissionsDto = sanitizeAccessPermissionsByDto(aclUnitDto.getPermissions());
		
		for(AccessPermissionDto accessPermissionDto : sanitizedAccessPermissionsDto) {
			accessPermissionsList.add(mapAccessPermissionDtoToAccessPermission(accessPermissionDto));
		}
		
		aclEntityDto.setAccessGrantedFrom(System.currentTimeMillis());
		aclEntityDto.setAccessGrantedTill(Long.MAX_VALUE);
		aclEntityDto.setPermissions(accessPermissionsList);
		
		return aclEntityDto;
	}
	
	public static List<AccessControlUnitEntityDto> mapAclUnitWebDtoToAclUnitEntityDto(AccessControlUnitWebDto aclUnitWebDto) {
		List<AccessControlUnitEntityDto> aclUnitEntityDtoList = new ArrayList<>();
		
		for(AccessControlUnitDto aclUnitDto : aclUnitWebDto.getAccessControlUnitDtos()) {
			AccessControlUnitEntityDto aclUnitEntityDto = new AccessControlUnitEntityDto();
			aclUnitEntityDto = mapAclUnitDtoToEntityDto(aclUnitDto);
			aclUnitEntityDtoList.add(aclUnitEntityDto);
		}
		return aclUnitEntityDtoList;
	}
	public static models.AccessArea mapAccessAreaDtoToAcessArea(dto.acl.AccessAreaDto accessAreaDto) {
		models.AccessArea accessArea = null;
		switch(accessAreaDto) {
			case EVENT :
				accessArea = models.AccessArea.EVENT;
				break;
			case JOB :
				accessArea = models.AccessArea.JOB;
				break;
			case ANNOUNCEMENT :
				accessArea = models.AccessArea.ANNOUNCEMENT;
				break;
			case ATTENDANCE :
				accessArea = models.AccessArea.ATTENDANCE;
				break;
			case DOWNLOAD :
				accessArea = models.AccessArea.DOWNLOAD;
				break;
			case PROFILE :
				accessArea = models.AccessArea.PROFILE;
				break;
		}
		return accessArea;
	}
	
	public static models.AccessPermission mapAccessPermissionDtoToAccessPermission(dto.acl.AccessPermissionDto accessPermissionDto) {
		models.AccessPermission accessPermission = null;
		switch(accessPermissionDto) {
			case MODIFY :
				accessPermission = models.AccessPermission.MODIFY;
				break;
			case ADMIN :
				accessPermission = models.AccessPermission.ADMIN;
				break;
			case PUBLISH :
				accessPermission = models.AccessPermission.PUBLISH;
				break;
			case REMOVE :
				accessPermission = models.AccessPermission.REMOVE;
				break;
			case VIEW :
				accessPermission = models.AccessPermission.VIEW;
				break;
		}
		return accessPermission;
	}
	
	public static String getNameFromEmail(String email) {
		String name = "";
		String[] emailSplits = email.split("@");
		if(emailSplits != null && emailSplits.length != 0) {			
			name = emailSplits[0];
		}
		return name;
	}
	
	public static List<AccessPermissionDto> sanitizeAccessPermissionsByDto(List<AccessPermissionDto> accessPermissions) {
		List<AccessPermissionDto> sanitizedAccessPermissions = new ArrayList<>();
		for(AccessPermissionDto accessPermission : accessPermissions) {
			List<AccessPermissionDto> allowedAccessPermissions = getAllowedAccessPermissionsByAccessPermissionDto(accessPermission);
			for(AccessPermissionDto allowedAccessPermission : allowedAccessPermissions) {
				if(!sanitizedAccessPermissions.contains(allowedAccessPermission)) {
					sanitizedAccessPermissions.add(allowedAccessPermission);
				}
			}
		}
		return sanitizedAccessPermissions;
	}
	
	public static List<AccessPermission> sanitizeAccessPermissions(List<AccessPermission> accessPermissions) {
		List<AccessPermission> sanitizedAccessPermissions = new ArrayList<>();
		for(AccessPermission accessPermission : accessPermissions) {
			List<AccessPermission> allowedAccessPermissions = getAllowedAccessPermissionsByAccessPermission(accessPermission);
			for(AccessPermission allowedAccessPermission : allowedAccessPermissions) {
				if(!sanitizedAccessPermissions.contains(allowedAccessPermission)) {
					sanitizedAccessPermissions.add(allowedAccessPermission);
				}
			}
		}
		return sanitizedAccessPermissions;
	}
	
	public static List<AccessPermissionDto> getAllowedAccessPermissionsByAccessPermissionDto(AccessPermissionDto accessPermission) {
		List<AccessPermissionDto> allowedAccessPermissions = new ArrayList<>();
		switch(accessPermission) {
			case ADMIN :
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.ADMIN);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.VIEW);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.MODIFY);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.PUBLISH);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.REMOVE);
				break;
			case PUBLISH :
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.PUBLISH);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.VIEW);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.MODIFY);
				break;
			case REMOVE :
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.REMOVE);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.VIEW);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.MODIFY);
				break;
			case MODIFY :
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.MODIFY);
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.VIEW);
				break;
			case VIEW :
				allowedAccessPermissions.add(dto.acl.AccessPermissionDto.VIEW);
				break;
		}
		return allowedAccessPermissions;
	}
	
	public static List<AccessPermission> getAllowedAccessPermissionsByAccessPermission(AccessPermission accessPermission) {
		List<AccessPermission> allowedAccessPermissions = new ArrayList<>();
		switch(accessPermission) {
			case ADMIN :
				allowedAccessPermissions.add(models.AccessPermission.ADMIN);
				allowedAccessPermissions.add(models.AccessPermission.VIEW);
				allowedAccessPermissions.add(models.AccessPermission.MODIFY);
				allowedAccessPermissions.add(models.AccessPermission.PUBLISH);
				allowedAccessPermissions.add(models.AccessPermission.REMOVE);
				break;
			case PUBLISH :
				allowedAccessPermissions.add(models.AccessPermission.PUBLISH);
				allowedAccessPermissions.add(models.AccessPermission.VIEW);
				allowedAccessPermissions.add(models.AccessPermission.MODIFY);
				break;
			case REMOVE :
				allowedAccessPermissions.add(models.AccessPermission.REMOVE);
				allowedAccessPermissions.add(models.AccessPermission.VIEW);
				allowedAccessPermissions.add(models.AccessPermission.MODIFY);
				break;
			case MODIFY :
				allowedAccessPermissions.add(models.AccessPermission.MODIFY);
				allowedAccessPermissions.add(models.AccessPermission.VIEW);
				break;
			case VIEW :
				allowedAccessPermissions.add(models.AccessPermission.VIEW);
				break;
		}
		return allowedAccessPermissions;
	}
	
	public static SubUserLogInDto createSubUserLogInDto(AccessControlListDto aclDto, TokenDto ownerTokenDto) {
		SubUserLogInDto subUserLoginDto = new SubUserLogInDto();
		MinimalUserDetailsDto ownerDetails = new MinimalUserDetailsDto();
		ownerDetails.setEmail(ownerTokenDto.getEmail());
		ownerDetails.setUuid(ownerTokenDto.getUuid());
		subUserLoginDto.setOwnerDetails(ownerDetails);
		SubUserDetailsDto subUserDetails = getSubUserDetailsDto(aclDto);
		subUserLoginDto.setSubUserDetails(subUserDetails);
		subUserLoginDto.setAclId(aclDto.getId());
		subUserLoginDto.setRefreshCounter(0l);
		return subUserLoginDto;
	}
	
	public static SubUserDetailsDto getSubUserDetailsDto(AccessControlListDto aclDto) {
		MinimalUserDetailsDto subUserDetails = aclDto.getSubUserDetails();
		SubUserDetailsDto subUserDetailsDto = new SubUserDetailsDto();
		subUserDetailsDto.setSubUserUuid(subUserDetails.getUuid());
		subUserDetailsDto.setSubUseremail(subUserDetails.getEmail());
		subUserDetailsDto.setPermissionDetails(aclDto.getAclUnits());
		return subUserDetailsDto;
	}
	
	public static String encryptAES(NinjaProperties ninjaProperties,String strToEncrypt) {
		try{
			
			String keyString = ninjaProperties.get("uus.aeskey");
			byte[] keyBytes = keyString.getBytes();
			SecretKey secretKey = new SecretKeySpec(keyBytes,"AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return new String(Base64.encodeBase64(cipher.doFinal(strToEncrypt.getBytes()),false,true));
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static String decryptAES(NinjaProperties ninjaProperties,String strToDecrypt) {
		try{
			String keyString = ninjaProperties.get("uus.aeskey");
			byte[] keyBytes = keyString.getBytes();;
			SecretKey secretKey = new SecretKeySpec(keyBytes,"AES");
            
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.decodeBase64(strToDecrypt)));
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean isValidaAccessArea(String accessArea) {
		switch(accessArea) {
			case "JOB" : return true;
			case "EVENT": return true;
			case "DOWNLOAD": return true;
			case "PROFILE": return true;
			case "ANNOUNCEMENT": return true;
			case "ATTENDANCE": return true;
		}
		return false;
	}
	
	public static boolean isValidaAccessPermission(String accessPermission) {
		switch(accessPermission) {
			case "ADMIN" : return true;
			case "MODIFY": return true;
			case "VIEW": return true;
			case "PUBLISH": return true;
			case "REMOVE": return true;
		}
		return false;
	}
	
	public static boolean isValidUusTokenType(String tokenType) {
		switch(tokenType) {
			case "OWNER": return true;
			case "SUB_USER": return true;
		}
		return false;
	}
	
	public static boolean areSameClasses(Class<?> classA, Class<?> classB) {
		 return classA.equals(classB);
	 }
	
	public static MinimalACLWebDto mapACLToMinimalAcl(AccessControlList acl) {
		MinimalACLWebDto minimalAclWebDto = new MinimalACLWebDto();
		minimalAclWebDto.setId(acl.getId());
		minimalAclWebDto.setSubUserId(acl.getSubUser().getUuid());
		minimalAclWebDto.setOwnerId(acl.getOwner().getUuid());
		minimalAclWebDto.setAclUnits(mapIndividualAclToAccessControlUntiList(acl));
		return minimalAclWebDto;
	}
}