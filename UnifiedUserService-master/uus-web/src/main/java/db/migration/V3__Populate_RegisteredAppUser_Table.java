package db.migration;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import models.IntermediateData;
import models.RegisteredApp;
import models.RegisteredAppUser;
import models.RolesEntityDto;
import models.UserDetailsEntityDto;
import models.UserRole;
import ninja.utils.NinjaMode;
import ninja.utils.NinjaPropertiesImpl;

public class V3__Populate_RegisteredAppUser_Table implements JdbcMigration {
	
	private final String mapitPrivateId = "mapit";
	private final String matchPrivateId = "match";
	
	@Override
	public void migrate(Connection connection) throws Exception {
		
	    EntityManager entityManager = getEntityManagerForWebDB();
	    
	    addRegisteredApps(entityManager);
	    
	    try {
	    	entityManager.getTransaction().begin();

	    	RegisteredApp mapitApp = getRegisteredAppByPrivateAppId(entityManager, mapitPrivateId);
		    RegisteredApp matchApp = getRegisteredAppByPrivateAppId(entityManager, matchPrivateId);
		    
		    List<IntermediateData> users = entityManager.createNamedQuery("IntermediateData.getAllUsers", IntermediateData.class).getResultList();
		    
		    for(IntermediateData user : users) {
		    	
		    	RegisteredAppUser rAppUser = new RegisteredAppUser();
		    	RolesEntityDto roles = new RolesEntityDto();
		    	UserDetailsEntityDto userDetails = new UserDetailsEntityDto();
		    	List<RegisteredApp> registeredApps = new ArrayList<>();
		    	
		    	if(user.getMapitEmail() != null) {
		    		
		    		rAppUser.setEmail(user.getMapitEmail());
		    		userDetails.setContactNumber(user.getMapitContactNumber());
		    		registeredApps.add(mapitApp);
		    		
		    		if(user.getMatchEmail() != null) {
		    			userDetails.setName(user.getMapitName()==null ? user.getMatchName() : user.getMapitName());
		    			roles.addRole(UserRole.valueOf(user.getMatchRole()));
		    			rAppUser.setActive(user.isMapitActive() || user.isMatchActive());
		    			rAppUser.setRegisteredAt(Math.min(user.getMapitRegisteredAt(), user.getMatchRegisteredAt()));
		    			registeredApps.add(matchApp);
		    			
		    		} else {
		    			userDetails.setName(user.getMapitName());
		    			roles.addRole(covertMapitRole(user.getMapitRole()));
		    			rAppUser.setActive(user.isMapitActive());
		    			rAppUser.setRegisteredAt(user.getMapitRegisteredAt());
		    		}
		    		
		    	} else {
		    		rAppUser.setEmail(user.getMatchEmail());
		    		roles.addRole(UserRole.valueOf(user.getMatchRole()));
		    		userDetails.setName(user.getMatchName());
		    		rAppUser.setActive(user.isMatchActive());
		    		rAppUser.setRegisteredAt(user.getMatchRegisteredAt());
		    		registeredApps.add(matchApp);
		    	}
		    	
		    	rAppUser.setUserDetails(userDetails);
		    	rAppUser.setRoles(roles);
		    	rAppUser.setRegisteredApps(registeredApps);
		    	rAppUser.setUserPasswordUpdated(false);
		    	entityManager.persist(rAppUser);
		    	
		    	user.setCopiedToRegisteredUser(true);
		    	entityManager.persist(user);
		    }
		    entityManager.getTransaction().commit();
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }
	    entityManager.close();
	}
	
	private void addRegisteredApps(EntityManager entityManager) {
		
		entityManager.getTransaction().begin();
		
		RegisteredApp mapit = new RegisteredApp();
		mapit.setName("MAPIT");
		mapit.setAppSecret("mapit");
		mapit.setPublicAppId("publicmapit");
		entityManager.persist(mapit);
		
		RegisteredApp match = new RegisteredApp();
		match.setName("match");
		match.setAppSecret("match");
		match.setPublicAppId("publicmatch");
		entityManager.persist(match);
		
		entityManager.getTransaction().commit();
	}

	private RegisteredApp getRegisteredAppByPrivateAppId(EntityManager entityManager, String privateAppId) {
		RegisteredApp app = entityManager.createNamedQuery("RegisteredApp.findByPrivateAppId", RegisteredApp.class)
									.setParameter("privateAppId", privateAppId)
									.getSingleResult();
		return app;
	}
	
	private UserRole covertMapitRole(String mapitRole) {
		if(mapitRole.equals("Hr")) {
			return UserRole.CORPORATE;
		} else if(mapitRole.equals("Candidate")){
			return UserRole.CANDIDATE;
		} else{
			throw new RuntimeException("Role does not match to neither candidate & hr");
		}
	}

	private EntityManager getEntityManagerForWebDB() {
		final NinjaPropertiesImpl ninjaProperties = new NinjaPropertiesImpl(NinjaMode.dev);
		String connectionUrl = ninjaProperties.get("db.connection.url");
		String connectionUsername = ninjaProperties.get("db.connection.username");
		String connectionPassword = ninjaProperties.get("db.connection.password");
		Properties jpaProperties = new Properties();
		jpaProperties.put("hibernate.connection.url", connectionUrl);
		jpaProperties.put("hibernate.connection.username", connectionUsername);
		jpaProperties.put("hibernate.connection.password", connectionPassword);
		
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("userservice_postgres", jpaProperties);
		EntityManager entityManager = emf.createEntityManager();
		return entityManager;
	}
}
