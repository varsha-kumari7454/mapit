package facade;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import caches.RegisteredAppCache;
import dto.DtoUtils;
import dto.RegisteredAppDto;
import exceptions.UusInvalidPublicAppId;
import exceptions.UusInvalidPublicOrPrivateAppId;
import models.RegisteredApp;
import ninja.utils.NinjaProperties;

public class AppFacadeImpl implements AppFacade {
	private static Logger log = LogManager.getLogger(AppFacadeImpl.class);

	@Inject
	private Provider<EntityManager> entityManagerProvider;
	@Inject
	private AppFacade self;

	@Inject
	NinjaProperties ninjaProperties;
	@Inject
	RegisteredAppCache registeredAppCache;

	@Override
	@Transactional
	public boolean isValidPublicOrPrivateAppId(String publicAppId, String privateAppId, Boolean noRedirect) {
		log.info("isValidPublicOrPrivateAppId called");
		if (noRedirect != null && noRedirect != true) {
			return false;
		}
		RegisteredAppDto registeredAppDtoFromCache = registeredAppCache.get(publicAppId);
		if(registeredAppDtoFromCache==null){
			EntityManager entityManager = entityManagerProvider.get();
			List<RegisteredApp> registeredApps = entityManager
					.createNamedQuery("RegisteredApp.findByPublicAppIdOrPrivateAppId", RegisteredApp.class)
					.setParameter("publicAppId", publicAppId)
					.setParameter("privateAppId", privateAppId)
					.getResultList();
			
			if (registeredApps == null || registeredApps.size() == 0) {
				log.info("No registered application found");
				return false;
			}
			if (registeredApps != null && registeredApps.size() != 1) {
				log.info("Invalid Request");
				return false;
			}
			RegisteredAppDto registeredAppDto = DtoUtils.mapToRegisteredAppDto(registeredApps.get(0));
			registeredAppCache.put(publicAppId, registeredAppDto);
			return true;
		}
		log.info("Found in cache");
		if(registeredAppDtoFromCache.getAppSecret().equals(privateAppId)){
			return true;
		}
		return false;
	}

	@Override
	@Transactional
	public boolean isValidPublicAppId(String publicAppId) {
		log.info("isValidPublicAppId called");
		RegisteredAppDto registeredAppDtoFromCache = registeredAppCache.get(publicAppId);
		if(registeredAppDtoFromCache==null){
			EntityManager entityManager = entityManagerProvider.get();
			List<RegisteredApp> registeredApps = entityManager
					.createNamedQuery("RegisteredApp.findByPublicAppId", RegisteredApp.class)
					.setParameter("publicAppId", publicAppId).getResultList();
			if (registeredApps == null || registeredApps.size() != 1) {
				return false;
			}
			RegisteredAppDto registeredAppDto = DtoUtils.mapToRegisteredAppDto(registeredApps.get(0));
			registeredAppCache.put(publicAppId, registeredAppDto);
		}
		log.info("Found in cache");
		return true;
	}

	@Override
	@Transactional
	public void checkRequestedAppValidity(String publicAppId,String privateAppId,Boolean noRedirect) {
		if (privateAppId == null && !self.isValidPublicAppId(publicAppId)) {
			throw new UusInvalidPublicAppId("Provided app id is Invalid");
		}
		if (privateAppId != null && !self.isValidPublicOrPrivateAppId(publicAppId, privateAppId, noRedirect)) {
			throw new UusInvalidPublicOrPrivateAppId("Provided app id is Invalid");
		}
	}
}