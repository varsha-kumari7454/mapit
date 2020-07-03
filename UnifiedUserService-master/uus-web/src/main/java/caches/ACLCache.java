package caches;

import dto.acl.MinimalACLWebDto;

public interface ACLCache extends CacheStat {

	void put(Long aclId, MinimalACLWebDto minimalACLWebDto);
	
	MinimalACLWebDto get(Long aclId);
	
	void remove(Long aclId);	
	
	void removeFromLocalCache(Long aclId);
	
	void emptyCache();
}
