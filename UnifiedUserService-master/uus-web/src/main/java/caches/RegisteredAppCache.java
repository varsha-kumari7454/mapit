package caches;


import dto.RegisteredAppDto;

public interface RegisteredAppCache extends CacheStat{
	void put(String appId, RegisteredAppDto registeredApp);
	RegisteredAppDto  get(String appId);
	void remove(String appId);	
	void removeFromLocalCache(String appId);
	void emptyCache();
}
