package dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import models.RegisteredApp;

public class SetupDao {
	@Inject
	private Provider<EntityManager> entityManagerProvider;

	@Transactional
	public void setup() {
		EntityManager entityManager = entityManagerProvider.get();
		List<RegisteredApp> registeredApps = entityManager
				.createQuery("SELECT  r FROM RegisteredApp r", RegisteredApp.class).setMaxResults('1').getResultList();
		if (registeredApps.size() > 0) {
			System.out.println("Initital data is already added");
			return;
		}
		RegisteredApp registeredApp = new RegisteredApp();
		registeredApp.setName("match");
		registeredApp.setAppSecret("matchappsecret");
		registeredApp.setLogoURL(
				"https://99designs-start-attachments.imgix.net/alchemy-pictures/2016%2F02%2F22%2F04%2F07%2F21%2F9757e437-5ec1-4378-804f-ca0f9567c110%2F380048_Widakk.png?auto=format&ch=Width%2CDPR&w=500&h=500");
		registeredApp.setPublicAppId("matchpublicappsecret");
		registeredApp.setUUID(1l);
		entityManager.persist(registeredApp);
		System.out.println("initial data added");
	}
}