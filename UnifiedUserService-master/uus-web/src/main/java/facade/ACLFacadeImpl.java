package facade;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import caches.ACLCache;
import dto.DtoUtils;
import dto.MinimalUserDetailsDto;
import dto.UserDto;
import dto.acl.AccessAreaDto;
import dto.acl.AccessControlListDto;
import dto.acl.AccessControlListWebDto;
import dto.acl.AccessControlUnitDto;
import dto.acl.AccessControlUnitWebDto;
import dto.acl.AccessPermissionDto;
import dto.acl.MinimalACLWebDto;
import exceptions.AclNotFoundException;
import exceptions.InvalidACLRequest;
import exceptions.InvalidAccessArea;
import exceptions.InvalidAccessPermission;
import exceptions.UusInvalidUserException;
import exceptions.UusSubUserNotFound;
import exceptions.UusSuperUserNotFound;
import models.AccessArea;
import models.AccessControlList;
import models.AccessControlUnitEntityDto;
import models.IndividualAccessControlListEntityDto;
import models.RegisteredAppUser;

public class ACLFacadeImpl implements ACLFacade {
	private static Logger log = LogManager.getLogger(ACLFacadeImpl.class);
	@Inject 
	Provider<EntityManager> entityManagerProvider;
	@Inject
	private UserFacade userFacade;
	@Inject
	private ACLFacade self;
	@Inject
	private ACLCache aclCache;
	@Inject
	private TokenFacade tokenFacade;
	
	@Override
	public AccessControlListWebDto getACL(UserDto userDto, boolean isGetByOwner) {
		log.info("getACL is called with userDto : "+userDto +" | isGetByOwner : "+isGetByOwner);
		
		RegisteredAppUser registeredAppUser = getRegisteredAppUserByUuid(userDto.getUuid());
		if(registeredAppUser == null) {
			log.error("Error : User not found");
			throw new UusSuperUserNotFound("User not found");
		}
		
		AccessControlListWebDto aclListWebDto = new AccessControlListWebDto();
		Long uuid = registeredAppUser.getUuid();
		
		List<AccessControlList> accessControlLists = new ArrayList<>();
		List<AccessControlListDto> accessControlListDto = new ArrayList<>();
		MinimalUserDetailsDto ownerDetails = new MinimalUserDetailsDto();
		
		if(isGetByOwner) {
			accessControlLists = getACLListsByOwnerId(uuid);
			for(AccessControlList acl : accessControlLists) {
				AccessControlListDto aclDto = new AccessControlListDto();
				if(acl != null) {		
					aclDto = DtoUtils.mapAccessControlListToDto(acl);
					aclDto.setSubUserId(acl.getSubUser().getUuid());
					MinimalUserDetailsDto subUserDetails = new MinimalUserDetailsDto();
					subUserDetails = DtoUtils.mapRegisteredAppUserToMinimalUserDetails(acl.getSubUser());
					aclDto.setSubUserDetails(subUserDetails);
				}
				accessControlListDto.add(aclDto);
			}
		} else {
			accessControlLists = getACLListsBySubUserId(uuid);
			for(AccessControlList acl : accessControlLists) {
				AccessControlListDto aclDto = new AccessControlListDto();
				if(acl != null) {		
					aclDto = DtoUtils.mapAccessControlListToDto(acl);
					aclDto.setSubUserId(acl.getOwner().getUuid());
					MinimalUserDetailsDto subUserDetails = new MinimalUserDetailsDto();
					subUserDetails = DtoUtils.mapRegisteredAppUserToMinimalUserDetails(acl.getOwner());
					aclDto.setSubUserDetails(subUserDetails);
				}
				accessControlListDto.add(aclDto);
			}
		}
		
		aclListWebDto.setAccessControlList(accessControlListDto);
		aclListWebDto.setOwnerId(uuid);
		ownerDetails = DtoUtils.mapRegisteredAppUserToMinimalUserDetails(registeredAppUser);
		aclListWebDto.setOwnerDetails(ownerDetails);
		
		return aclListWebDto;
	}

	@Override
	public void updateACL(UserDto userDto, MinimalACLWebDto minimalAclDto) throws InvalidAccessArea, InvalidAccessPermission, InvalidACLRequest {
		log.info("updateACL is called with userDto : "+userDto);
		
		if(minimalAclDto == null || minimalAclDto.getAclUnits() == null || minimalAclDto.getAclUnits().size() == 0) {
			log.error("Error : Not provided any access areas and permissions to add");
			throw new InvalidACLRequest("Not provided any access areas and permissions to add");
		}
		
		validateAccessAreaNdAccessPermission(minimalAclDto.getAclUnits());
		
		Long aclId = minimalAclDto.getId();
		AccessControlList accessControlList = getACLById(aclId);
		
		if(accessControlList == null) {
			log.error("Error : Requesting ACL is not found to update");
			throw new AclNotFoundException("Requested Access Control List Not Found to Update");
		}
		
		RegisteredAppUser registeredOwnerUser = accessControlList.getOwner();
		if(registeredOwnerUser == null) {
			log.error("Error : Owner not found");
			throw new UusSuperUserNotFound("Owner User not found");
		}
		
		RegisteredAppUser registeredSubUser = accessControlList.getSubUser();
		if(registeredSubUser == null) {
			log.error("Error : SubUser not found");
			throw new UusSubUserNotFound("Sub User not found");
		}
		updateAccessControlList(minimalAclDto, aclId);
	}

	@Override
	@Transactional
	public void updateAccessControlList(MinimalACLWebDto minimalAclDto, Long aclId) {
		log.info("updateAccessControlList is Called");
		EntityManager entityManager = entityManagerProvider.get();
		AccessControlList accessControlList = getACLById(aclId);
		List<AccessControlUnitEntityDto> updatedAccessControlUnitsList = new ArrayList<AccessControlUnitEntityDto>();
		IndividualAccessControlListEntityDto individualAclList = accessControlList.getiACL();
		List<AccessControlUnitEntityDto> accessControlUnitsList = individualAclList.getAccessControlUnit();
		
		for(AccessControlUnitDto aclUnitWebDto : minimalAclDto.getAclUnits()) {
			AccessControlUnitEntityDto aclUnitEntityDto = new AccessControlUnitEntityDto();
			aclUnitEntityDto = DtoUtils.mapAclUnitDtoToEntityDto(aclUnitWebDto);
			updatedAccessControlUnitsList.add(aclUnitEntityDto);
		}
		
		individualAclList.setAccessControlUnit(updatedAccessControlUnitsList);
		accessControlList.setiACL(individualAclList);
		entityManager.persist(accessControlList);

		aclCache.remove(aclId);
		addToAclCacheIfNotPresent(accessControlList);
	}

	@Override
	public void addACL(UserDto userDto, String subUserEmail, AccessControlUnitWebDto aclUnitWebDto) throws InvalidAccessArea, InvalidAccessPermission, InvalidACLRequest {
		log.info("addACL is called with userDto : "+userDto+" | subUserEmail : "+subUserEmail);
		if(aclUnitWebDto == null || aclUnitWebDto.getAccessControlUnitDtos() == null || aclUnitWebDto.getAccessControlUnitDtos().size() == 0) {
			log.error("Error : Not provided any access areas and permissions to add");
			throw new InvalidACLRequest("Not provided any access areas and permissions to add");
		}
		validateAccessAreaNdAccessPermission(aclUnitWebDto.getAccessControlUnitDtos());
		
		RegisteredAppUser registeredOwner = getRegisteredAppUserByUuid(userDto.getUuid());
		if(registeredOwner == null) {
			log.error("Error : Owner not found");
			throw new UusSuperUserNotFound("Owner User not found");
		}
		RegisteredAppUser registeredSubUser = getRegisteredAppUserByEmail(subUserEmail);
		if(registeredSubUser == null) {
			log.error("Error : SubUser not found");
			throw new UusSubUserNotFound("Sub User not found");
		}

		Long ownerId = registeredOwner.getUuid();
		Long subUserId = registeredSubUser.getUuid();
		AccessControlList accessControlList = getACLListsByOwnerIdNdSubUserId(ownerId, subUserId);
		if(accessControlList != null) {
			log.info("User already added permissions for this subuser so returning them: ");
			return;
		}
		log.info("User is adding permissions for this subuser newly: ");
		addNewAcl(registeredOwner, registeredSubUser, aclUnitWebDto);
		updateSubUserAfterAddingAcl(registeredSubUser.getUuid());
		flushUusTokensByUuid(subUserId);
	}

	@Override
	@Transactional
	public void updateSubUserAfterAddingAcl(Long subUserUuid) {
		log.info("updateSubUserAfterAddingAcl is Called with subUserUuid : "+subUserUuid);
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredAppUser registeredSubUser = getRegisteredAppUserByUuid(subUserUuid);
		boolean active = true;
		Long superUsersCount = registeredSubUser.getSuperUsersCount();
		superUsersCount++;
		boolean createdAsSubUser = registeredSubUser.isCreatedAsSubUser();
		if(superUsersCount > 0 && createdAsSubUser) {
			active = true;
			registeredSubUser.setActive(active);
		}
		if(superUsersCount < 0) {
			log.info("superUsersCount is less than Zero so, making it Zero");
			superUsersCount = 0l;
		}
		registeredSubUser.setSuperUsersCount(superUsersCount);
		entityManager.persist(registeredSubUser);
	}

	@Override
	@Transactional
	public void addNewAcl(RegisteredAppUser registeredOwner, RegisteredAppUser registeredSubUser, AccessControlUnitWebDto aclUnitWebDto) {
		log.info("addNewAcl is Called");
		EntityManager entityManager = entityManagerProvider.get();
		IndividualAccessControlListEntityDto individualAclList = new IndividualAccessControlListEntityDto();
		List<AccessControlUnitEntityDto> accessControlUnitsList = new ArrayList<>();
		
		AccessControlList accessControlList = new AccessControlList();
		accessControlList.setOwner(registeredOwner);
		accessControlList.setSubUser(registeredSubUser);
		accessControlList.setCreationTime(System.currentTimeMillis());
		accessControlList.setLastmodifiedTime(System.currentTimeMillis());
		
		accessControlUnitsList = DtoUtils.mapAclUnitWebDtoToAclUnitEntityDto(aclUnitWebDto);
		individualAclList.setAccessControlUnit(accessControlUnitsList);
		
		accessControlList.setiACL(individualAclList);
		entityManager.persist(accessControlList);
		addToAclCacheIfNotPresent(accessControlList);
	}
	
	@Override
	public void deleteACL(UserDto userDto, MinimalACLWebDto minimalAclDto) throws InvalidACLRequest {
		log.info("deleteACL is called with userDto : "+userDto);

		if(minimalAclDto == null) {
			log.error("Error : Not provided any data to delete Access Permissions");
			throw new InvalidACLRequest("Not provided any data to delete Access Permissions");
		}
		
		Long aclId = minimalAclDto.getId();
		AccessControlList accessControlList = getACLById(aclId);
		if(accessControlList == null) {
			throw new AclNotFoundException("Requested Access Control List Not Found");
		}
		
		RegisteredAppUser registeredOwnerUser = accessControlList.getOwner();
		if(registeredOwnerUser == null) {
			log.error("Error : Owner not found");
			throw new UusSuperUserNotFound("Owner User not found");
		}
		
		RegisteredAppUser registeredSubUser = accessControlList.getSubUser();
		if(registeredSubUser == null) {
			log.error("Error : SubUser not found");
			throw new UusSubUserNotFound("Sub User not found");
		}
		
		deleteAcl(aclId);
		updateSubUserAfterDeletingAcl(accessControlList.getSubUser().getUuid());
		flushUusTokensByUuid(minimalAclDto.getSubUserId());
	}

	@Override
	@Transactional
	public void deleteAcl(Long aclId) {
		EntityManager entityManager = entityManagerProvider.get();
		AccessControlList accessControlList = getACLById(aclId);
		entityManager.remove(accessControlList);
		aclCache.remove(aclId);
	}

	@Override
	@Transactional
	public void updateSubUserAfterDeletingAcl(Long subUserUuid) {
		EntityManager entityManager = entityManagerProvider.get();
		RegisteredAppUser registeredSubUser = getRegisteredAppUserByUuid(subUserUuid);
		boolean active = true;
		Long superUsersCount = registeredSubUser.getSuperUsersCount();
		--superUsersCount;
		
		boolean createdAsSubUser = registeredSubUser.isCreatedAsSubUser();
		if(superUsersCount <= 0 && createdAsSubUser) {
			active = false;
		}
		if(superUsersCount < 0) {
			log.info("superUsersCount is less than Zero so, making it Zero");
			superUsersCount = 0l;
		}
		registeredSubUser.setActive(active);
		registeredSubUser.setSuperUsersCount(superUsersCount);
		entityManager.persist(registeredSubUser);
	}

	@Override
	public AccessControlListWebDto getAclWebDtoByOwner(RegisteredAppUser registeredOwnerUser) {
		Long ownerId = registeredOwnerUser.getUuid();
		log.info("getAclWebDtoByOwnerId is Called with ownerId : "+ownerId);
		AccessControlListWebDto aclWebDto = new AccessControlListWebDto();
		List<AccessControlList> accessControlLists = getACLListsByOwnerId(ownerId);
		List<AccessControlListDto> accessControlListDto = new ArrayList<>();
		
		for(AccessControlList acl : accessControlLists) {
			AccessControlListDto aclDto = new AccessControlListDto();
			if(acl != null) {			
				aclDto = DtoUtils.mapAccessControlListToDto(acl);
				aclDto.setSubUserId(acl.getSubUser().getUuid());
				MinimalUserDetailsDto subUserDetails = new MinimalUserDetailsDto();
				subUserDetails = DtoUtils.mapRegisteredAppUserToMinimalUserDetails(acl.getSubUser());
				aclDto.setSubUserDetails(subUserDetails);
			}
			accessControlListDto.add(aclDto);
		}
		aclWebDto.setAccessControlList(accessControlListDto);
		
		aclWebDto.setOwnerId(ownerId);
		MinimalUserDetailsDto ownerDetails = new MinimalUserDetailsDto();
		ownerDetails = DtoUtils.mapRegisteredAppUserToMinimalUserDetails(registeredOwnerUser);
		aclWebDto.setOwnerDetails(ownerDetails);
		
		return aclWebDto;
	}
	
	@Override
	public AccessControlListWebDto getAclWebDtoByOwnerUserDto(UserDto userDto) {
		Long ownerId = userDto.getUuid();
		log.info("getAclWebDtoByOwnerUserDto is Called with ownerId : "+ownerId);
		AccessControlListWebDto aclWebDto = new AccessControlListWebDto();
		List<AccessControlList> accessControlLists = getACLListsByOwnerId(ownerId);
		List<AccessControlListDto> accessControlListDto = new ArrayList<>();
		
		for(AccessControlList acl : accessControlLists) {
			AccessControlListDto aclDto = new AccessControlListDto();
			if(acl != null) {			
				aclDto = DtoUtils.mapAccessControlListToDto(acl);
				aclDto.setSubUserId(acl.getSubUser().getUuid());
				MinimalUserDetailsDto subUserDetails = new MinimalUserDetailsDto();
				subUserDetails = DtoUtils.mapRegisteredAppUserToMinimalUserDetails(acl.getSubUser());
				aclDto.setSubUserDetails(subUserDetails);
			}
			accessControlListDto.add(aclDto);
		}
		aclWebDto.setAccessControlList(accessControlListDto);
		
		aclWebDto.setOwnerId(ownerId);
		MinimalUserDetailsDto ownerDetails = new MinimalUserDetailsDto();
		ownerDetails = DtoUtils.mapUserDtoToMinimalUserDetails(userDto);
		aclWebDto.setOwnerDetails(ownerDetails);
		return aclWebDto;
	}
	
	private RegisteredAppUser getRegisteredAppUserByEmail(String userEmail) {
		RegisteredAppUser registeredAppUser = userFacade.getRegisteredUserByEmail(userEmail);
		return registeredAppUser;
	}
	
	private RegisteredAppUser getRegisteredAppUserByUuid(Long subUserUuid) {
		RegisteredAppUser registeredAppUser = userFacade.getRegisteredAppUserByUuid(subUserUuid);
		return registeredAppUser;
	}
	
	@Override
	@Transactional
	public List<AccessControlList> getACLListsByOwnerId(Long ownerId) {
		EntityManager entityManager = entityManagerProvider.get();
		log.info("getACLListsByOwnerId is called with OwnerId : "+ownerId);
		List<AccessControlList> accessControlLists = entityManager
				.createNamedQuery("AccessControlList.findByOwnerId", AccessControlList.class)
				.setParameter("ownerId", ownerId)
				.getResultList();
		
		return accessControlLists;
	}
	
	@Override
	@Transactional
	public List<AccessControlList> getACLListsBySubUserId(Long subUserId) {
		EntityManager entityManager = entityManagerProvider.get();
		log.info("getACLListsBySubUserId is called with subUserId : "+subUserId);
		List<AccessControlList> accessControlLists = entityManager
				.createNamedQuery("AccessControlList.findBySubUserId", AccessControlList.class)
				.setParameter("subUserId", subUserId)
				.getResultList();
		
		return accessControlLists;
	}
	
	@Override
	@Transactional
	public AccessControlList getACLListsByOwnerIdNdSubUserId(Long ownerId, Long subUserId) {
		log.info("getACLListsBySubUserId is called with subUserId : "+subUserId);
		EntityManager entityManager = entityManagerProvider.get();
		List<AccessControlList> accessControlLists = entityManager
						.createNamedQuery("AccessControlList.findByOwnerIdNdSubUserId", AccessControlList.class)
						.setParameter("subUserId", subUserId)
						.setParameter("ownerId", ownerId)
						.getResultList();
		if(accessControlLists != null && accessControlLists.size() == 1) {
			return accessControlLists.get(0);
		}
		return null;
	}
	
	@Override
	@Transactional
	public AccessControlList getACLById(Long aclId) {
		log.info("getACLById is called with aclId : "+aclId);
		EntityManager entityManager = entityManagerProvider.get();
		AccessControlList accessControlList = entityManager.find(AccessControlList.class, aclId);
		if(accessControlList != null) {
			return accessControlList;
		}
		return null;
	}
	
	//Subjected to use only for SubUserToken Creation
	@Override
	public AccessControlListDto getACLByOwnerNdSubUserIdsForSubUserToken(Long ownerUuid, Long subUserUuid) {
		log.info("getACLByOwnerNdSubUserIds is Called with ownerUuid : "+ ownerUuid + " | subUserUuid : "+subUserUuid);
		AccessControlListDto aclDto = new AccessControlListDto();
		AccessControlList accessControlList = getACLListsByOwnerIdNdSubUserId(ownerUuid, subUserUuid);
		if(accessControlList == null) {
			log.error("Error : NO ACLs are found for SubUser");
			throw new AclNotFoundException("No Access has been to this Sub User by Owner.");
		}
		aclDto = DtoUtils.mapAccessControlListToDto(accessControlList);
		
		/**
		 * While Creating the subUserToken, Actual Sub User becomes Owner and Vice Versa
		 */
		aclDto.setSubUserId(accessControlList.getOwner().getUuid());
		MinimalUserDetailsDto subUserDetails = new MinimalUserDetailsDto();
		subUserDetails = DtoUtils.mapRegisteredAppUserToMinimalUserDetails(accessControlList.getOwner());
		aclDto.setSubUserDetails(subUserDetails);
		return aclDto;
	}
	
	private void validateAccessAreaNdAccessPermission(List<AccessControlUnitDto> accessControlUnitDtos) 
			throws InvalidAccessArea, InvalidAccessPermission {
		for(AccessControlUnitDto acl : accessControlUnitDtos) {
			boolean isValidAccessArea = DtoUtils.isValidaAccessArea(acl.getArea().toString());
			
			if(!isValidAccessArea) {
				log.error("Error : "+acl.getArea().toString()+" is Not a valid Access Area");
				throw new InvalidAccessArea(acl.getArea().toString()+" is not a valid access area");
			}
			
			for(AccessPermissionDto accessPermission : acl.getPermissions()) {				
				boolean isValidAccessPermission = DtoUtils.isValidaAccessPermission(accessPermission.toString());
				
				if(!isValidAccessPermission) {
					log.error("Error : "+accessPermission.toString()+" is Not a valid Access Permission");
					throw new InvalidAccessPermission(accessPermission.toString()+" is not a valid access permission");
				}
			}
		}
	}
	
	private void addToAclCacheIfNotPresent(AccessControlList acl) {
		Long aclId = acl.getId();
		MinimalACLWebDto minimalAclWebDto = aclCache.get(aclId);
		if(minimalAclWebDto == null) {
			minimalAclWebDto = DtoUtils.mapACLToMinimalAcl(acl);
			aclCache.put(aclId, minimalAclWebDto);
 		} 
	}
	
	@Override
	public MinimalACLWebDto getAclFromCache(Long aclId) {
		log.info("getAclFromCache is Called with aclId : "+aclId);
		MinimalACLWebDto minimalAclWebDto = aclCache.get(aclId);
		if(minimalAclWebDto == null) {
			log.info("ACL details not found in Cache so trying to load from DB");
			minimalAclWebDto = getMinimalAclNdAddToCache(aclId);
			if(minimalAclWebDto != null) {
				aclCache.put(aclId, minimalAclWebDto);
			} else {
				log.info("ACL with Id "+aclId+" is not found found  in DB so returning NULL");
			}
		} else {
			log.info("Found ACL details in Cache so returning them");
		}
		return minimalAclWebDto;
	}
	
	@Override
	@Transactional
	public MinimalACLWebDto getMinimalAclNdAddToCache(Long aclId) {
		log.info("getMinimalAclNdAddToCache is Called with aclId : "+aclId);
		EntityManager entityManager = entityManagerProvider.get();
		AccessControlList acl = entityManager.find(AccessControlList.class, aclId);
		if(acl == null) {
			return null;
		}
		MinimalACLWebDto minimalACLWebDto = DtoUtils.mapACLToMinimalAcl(acl);
		return minimalACLWebDto;
	}
	
	private void flushUusTokensByUuid(Long uuid) {
		tokenFacade.flushTokensByUuid(uuid);
	}
	
}
