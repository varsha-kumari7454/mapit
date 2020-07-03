package controllers;

import ninja.Result;
import ninja.Results;
import ninja.params.PathParam;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import caches.CacheInvalidator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CacheClearanceListenerController {
	private static Logger log = LogManager.getLogger(CacheClearanceListenerController.class);	
	@Inject
	private CacheInvalidator cacheInvalidator;
	public Result clearCache(@PathParam("cacheClass") String cacheClass, @PathParam("key") String key) {
		try {
		cacheInvalidator.invalidateLocalCache(cacheClass, key);
		return Results.ok().json().render("cleared");
		}catch(Exception e) {
			return Results.internalServerError().json().render(e.getMessage());
		}
	}
	
	public Result clearCacheFromAllServers(@PathParam("cacheClass") String cacheClass, @PathParam("key") String key) {
		try {
			if(key == null || key.equals("null")){
				log.debug("clear complete cache as key is null");
				//"cleared complete cache for "+cacheClass
				return Results.ok().json().render(cacheInvalidator.invalidateLocalCacheFromAllServers(cacheClass, null));
			}else{
				return Results.ok().json().render(cacheInvalidator.invalidateLocalCacheFromAllServers(cacheClass, key));
			}
		}catch(Exception e) {
			return Results.internalServerError().json().render(e.getMessage());
		}
	}
	
	public Result getCacheStat() {
		return Results.ok().json().render(cacheInvalidator.getStats());
	}
}
