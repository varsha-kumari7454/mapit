package facade;

import java.util.List;

import dto.UserDto;
import dto.acl.AccessControlListDto;
import dto.acl.AccessControlListWebDto;
import dto.acl.AccessControlUnitDto;
import dto.acl.AccessControlUnitWebDto;
import dto.acl.MinimalACLWebDto;
import exceptions.InvalidACLRequest;
import exceptions.InvalidAccessArea;
import exceptions.InvalidAccessPermission;
import models.AccessControlList;
import models.RegisteredAppUser;

public interface ACLFacade {

	AccessControlListWebDto getACL(UserDto userDto, boolean isGetByOwner);

	void updateACL(UserDto userDto, MinimalACLWebDto minimalAclDto)
			throws InvalidAccessArea, InvalidAccessPermission, InvalidACLRequest;

	void addACL(UserDto userDto, String subUserEmail, AccessControlUnitWebDto aclUnitWebDto)
			throws InvalidAccessArea, InvalidAccessPermission, InvalidACLRequest;

	void deleteACL(UserDto userDto, MinimalACLWebDto minimalAclDto) throws InvalidACLRequest;

	AccessControlListWebDto getAclWebDtoByOwner(RegisteredAppUser owner);

	AccessControlListDto getACLByOwnerNdSubUserIdsForSubUserToken(Long ownerUuid, Long subUserUuid);

	MinimalACLWebDto getAclFromCache(Long aclId);

	AccessControlListWebDto getAclWebDtoByOwnerUserDto(UserDto userDto);

	void updateAccessControlList(MinimalACLWebDto minimalAclDto, Long aclId);

	void updateSubUserAfterAddingAcl(Long subUserUuid);

	void addNewAcl(RegisteredAppUser registeredOwner, RegisteredAppUser registeredSubUser,
			AccessControlUnitWebDto aclUnitWebDto);

	void deleteAcl(Long aclId);

	void updateSubUserAfterDeletingAcl(Long subUserUuid);

	List<AccessControlList> getACLListsByOwnerId(Long ownerId);

	List<AccessControlList> getACLListsBySubUserId(Long subUserId);

	AccessControlList getACLListsByOwnerIdNdSubUserId(Long ownerId, Long subUserId);

	AccessControlList getACLById(Long aclId);

	MinimalACLWebDto getMinimalAclNdAddToCache(Long aclId);

}
