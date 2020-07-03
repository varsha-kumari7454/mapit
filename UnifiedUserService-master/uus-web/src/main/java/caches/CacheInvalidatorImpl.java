package caches;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import dto.Pair;
import ninja.utils.NinjaProperties;


public class CacheInvalidatorImpl implements CacheInvalidator, Runnable {
	private static Logger log = LogManager.getLogger(CacheInvalidatorImpl.class);
	
	@Inject
	@Named("cacheInvalidateQueue")
	private LinkedBlockingQueue<Pair> cacheInvalidateQueue;
	@Inject
	private NinjaProperties ninja;
	@Inject
	private CloseableHttpClient httpClient;
	@Inject
	private RegisteredAppUserCache registeredAppUserCache;
	@Inject
	private RegisteredAppCache registeredAppCache;
	@Inject
	private ACLCache aclCache;
	
	
	final private String registeredAppUserCacheRoute = "RegisteredAppUserCache";
	final private String registeredAppCacheRoute = "RegisteredAppCache";
	final private String aclCacheRoute = "ACLCache";
	
	@Override
	public void invalidateCacheNonBlocking(Class<?> clazz, Object key) {
		log.debug("called invalidateCacheNonBlocking");		
		addToQueue(clazz, key);  
	}
	
	private void addToQueue(Class<?> clazz, Object key) {
		log.debug("addToQueue called");
		cacheInvalidateQueue.add(new Pair(clazz.getSimpleName(), key));		
	}

	@Override
	public void run() {
		while(true) {
			if(Thread.interrupted()){
				log.debug("CacheInvalidatorImpl thread intrupted");
				break;
			}
			try {
				Pair req = cacheInvalidateQueue.poll(Integer.MAX_VALUE, TimeUnit.DAYS);
				invalidateLocalCacheFromAllServers((String)req.getA(), req.getB().toString());
			} catch (InterruptedException e) {
				log.error("error",e);
			} catch (Exception e) {				
				log.error("error",e);
			}
		}	
	}
	
	
	@Override
	public void invalidateLocalCache(String cacheClass, String key) {	
		log.debug("invalidateLocalCache called with cacheClass : " + cacheClass + " & Key : " + key);
		if(key == null || key.equals("null")) {
			key= null;
		}
		if(cacheClass.equals(registeredAppUserCacheRoute)) {
			if(key == null){
				registeredAppUserCache.emptyCache();
			}else{
				registeredAppUserCache.removeFromLocalCache(key);
			}
		} else if(cacheClass.equals(registeredAppCacheRoute)) {
			if(key == null){
				registeredAppCache.emptyCache();
			}else{
				registeredAppCache.removeFromLocalCache(key);
			}
		} else if(cacheClass.equals(aclCacheRoute)) {
			if(key == null){
				aclCache.emptyCache();
			}else{
				aclCache.removeFromLocalCache(Long.parseLong(key));
			}
		}
		else {
			log.error("error unknow cache clearance request:" + cacheClass+", "+ key);
		}
	}
	
	@Override
	public String invalidateLocalCacheFromAllServers(String cacheClass, String key){	
		log.debug("invalidateLocalCacheFromAllServers called for : " + cacheClass);		
		String[] serverArray = ninja.getStringArray("myanatomy.webServers");		
		if(serverArray == null || serverArray.length == 0) {
			log.debug("Cant find serverArray in cacheInvalidator. Please set myanatomy.webServers with the list of the servers");
			return "No myanatomy.webServers found. Please check in the application.conf";
		}
		for (String url : serverArray) {
			url = url + "/clearcache";
			if(cacheClass.equals(RegisteredAppUserCache.class.getSimpleName())) {
				url = url + "/" + registeredAppUserCacheRoute + "/" + key;
				makeDeleteRequest(url);
			} else if(cacheClass.equals(RegisteredAppCache.class.getSimpleName())) {
				url = url + "/" + registeredAppCacheRoute + "/" + key;
				makeDeleteRequest(url);
			} else if(cacheClass.equals(ACLCache.class.getSimpleName())) {
				url = url + "/" + aclCacheRoute + "/" + key;
				makeDeleteRequest(url);
			} else {
				log.error("unknown cache clearing request" + cacheClass + key);
				return "the class name " + cacheClass  + " is invalid. Please user the cache class. Ex: AppUserCache";
			}
		}
		return "cleared";
	}
	
	
	@Override
	public Map<String, Long> getStats() {
		Map<String, Long> ans = new HashMap<>();
		ans.put(registeredAppUserCacheRoute, registeredAppUserCache.getSize());
		ans.put(registeredAppCacheRoute, registeredAppCache.getSize());
		ans.put(aclCacheRoute, aclCache.getSize());
		return ans;
	}
	
	private void makeDeleteRequest(String url) {
		CloseableHttpResponse response = null;
		try {
			HttpGet get = new HttpGet(url);			
			response = httpClient.execute(get);
			log.debug("reading entity from  response");
			response.getEntity();
			log.debug("done reading entity from  response");			
		} catch (IOException e) {
			log.error("something went wrong while processing request to clearCache", e);						
		} finally {
			try {
				response.close();
			} catch (Exception e) {}
		}
	}	
}
