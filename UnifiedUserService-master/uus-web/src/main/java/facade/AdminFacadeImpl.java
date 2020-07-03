package facade;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import dto.AdminUserDto;
import dto.AdminUsersDto;

import javax.persistence.EntityManager;

import org.apache.commons.io.IOUtils;
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
import models.EmailChangeTracker;

import com.google.gson.Gson;

public class AdminFacadeImpl implements AdminFacade {
	private static Logger log = LogManager.getLogger(AppFacadeImpl.class);
	
	@Inject
	private Provider<EntityManager> entityManagerProvider;

	@Inject
	NinjaProperties ninjaProperties;

	private Gson gson = new Gson();

	@Override
	@Transactional
	public AdminUsersDto getAdminUsers() throws IOException {
		log.info("getAdminUsers called..");
		AdminUsersDto UsersDto = new AdminUsersDto();
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("AdminUsers.json");
		String adminUsersListString = IOUtils.toString(in, "UTF-8");
		UsersDto = gson.fromJson(adminUsersListString, AdminUsersDto.class);
		return UsersDto;
	}

	@Override
	@Transactional
	public List<EmailChangeTracker> getEmailUpdateHistory() throws IOException {
		log.info("getAdminUsers called..");
		EntityManager entityManager = entityManagerProvider.get();
		List<EmailChangeTracker> emailChangeTracker = entityManager
				.createQuery("Select x from EmailChangeTracker x Order by x.id desc", EmailChangeTracker.class)
				.getResultList();
		return emailChangeTracker;
	}
	
}