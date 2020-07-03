package controllers;

import dto.acl.AccessControlListDto;
import dto.acl.AccessControlUnitDto;
import dto.acl.AccessControlUnitWebDto;
import dto.acl.MinimalACLWebDto;
import ninja.Result;

public interface AccessControlListController {

	Result getACLByOwner(String uusToken);

	Result updateACL(String uusToken, MinimalACLWebDto minimalAclDto);

	Result addACL(String uusToken, String subUserEmail, String subUserName, AccessControlUnitWebDto aclUnitWebDto);

	Result deleteACL(String uusToken, MinimalACLWebDto minimalAclDto);

	Result getACLBySubUser(String uusToken);
}
