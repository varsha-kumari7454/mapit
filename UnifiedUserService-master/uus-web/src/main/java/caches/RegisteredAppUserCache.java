package caches;

import dto.UserDto;

public interface RegisteredAppUserCache extends CacheStat{
	void put(String email, UserDto user);
	UserDto  get(String email);
	void remove(String email);	
	void removeFromLocalCache(String email);
	void emptyCache();
}
