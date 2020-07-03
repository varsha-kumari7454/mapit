package caches;

import java.util.Map;

public interface CacheInvalidator {

	void invalidateCacheNonBlocking(Class<?> clazz, Object key);

	void invalidateLocalCache(String cacheClass, String key);

	String invalidateLocalCacheFromAllServers(String cacheClass, String key);

	Map<String, Long> getStats();	

}
