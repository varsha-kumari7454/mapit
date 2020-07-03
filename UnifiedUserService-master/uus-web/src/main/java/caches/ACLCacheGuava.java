package caches;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

import dto.acl.MinimalACLWebDto;

public class ACLCacheGuava implements ACLCache{
	private static Logger log = LogManager.getLogger(ACLCacheGuava.class);
	@Inject
	private CacheInvalidator cacheInvalidator;
	private static Cache<Long, MinimalACLWebDto> cache = null;
	static {
		cache = CacheBuilder.newBuilder()
		       .maximumSize(500)		       
		       .expireAfterAccess(1, TimeUnit.DAYS)		       
		       .build();
	}
	
	@Override
	public long getSize() {
		System.out.println("********************* "+ cache.toString());
		return cache.asMap().size();
	}

	@Override
	public void put(Long aclId, MinimalACLWebDto minimalACLWebDto) {
		cache.put(aclId, minimalACLWebDto);
		
	}

	@Override
	public MinimalACLWebDto get(Long aclId) {
		return cache.getIfPresent(aclId);
	}

	@Override
	public void remove(Long aclId) {
		cacheInvalidator.invalidateCacheNonBlocking(MinimalACLWebDto.class, aclId);
		removeFromLocalCache(aclId);
		
	}

	@Override
	public void removeFromLocalCache(Long aclId) {
		if(aclId == null) {
			emptyCache();
		} else {
			cache.invalidate(aclId);
		}
	}

	@Override
	public void emptyCache() {
		System.out.println("EMPTY CACHE CALLED********************* "+ cache.toString());
		cache = CacheBuilder.newBuilder()
			       .maximumSize(500)		       
			       .expireAfterAccess(1, TimeUnit.DAYS)		       
			       .build();
	}

}
