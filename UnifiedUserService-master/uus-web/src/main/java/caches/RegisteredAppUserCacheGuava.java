package caches;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import dto.UserDto;

@Singleton
public class RegisteredAppUserCacheGuava implements RegisteredAppUserCache {
	
	private static Logger log = LogManager.getLogger(RegisteredAppUserCacheGuava.class);
	@Inject
	private CacheInvalidator cacheInvalidator;
	private static Cache<String, UserDto> cache = null;
	static {
		cache = CacheBuilder.newBuilder()
		       .maximumSize(200)		       
		       .expireAfterAccess(20, TimeUnit.MINUTES)		       
		       .build();
	}
	
	@Override
	public void put(String email, UserDto userDto) {
		cache.put(email, userDto);

	}

	@Override
	public UserDto get(String email) {
		return cache.getIfPresent(email);
	}

	@Override
	public void remove(String email) {
		cacheInvalidator.invalidateCacheNonBlocking(UserDto.class, email);
		removeFromLocalCache(email);

	}

	@Override
	public void removeFromLocalCache(String email) {
		if(email == null) {
			emptyCache();
		} else {
			cache.invalidate(email);
		}
	}
	
	@Override
	public void emptyCache(){
		System.out.println("EMPTY CACHE CALLED*********************8"+ cache.toString());
		cache = CacheBuilder.newBuilder()
			       .maximumSize(200)		       
			       .expireAfterAccess(20, TimeUnit.MINUTES)		       
			       .build();
	}
		
	@Override
	public long getSize() {
		System.out.println("*********************8"+ cache.toString());
		return cache.asMap().size();	
	}
}
