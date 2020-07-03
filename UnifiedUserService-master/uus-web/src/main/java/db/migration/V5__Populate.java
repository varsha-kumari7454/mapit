package db.migration;
import java.sql.Connection;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import models.MetaDataEntityDto;
import models.RegisteredApp;
import ninja.utils.NinjaMode;
import ninja.utils.NinjaPropertiesImpl;

public  class V5__Populate implements JdbcMigration{

	@Override
	public void migrate(Connection connection) throws Exception {
		EntityManager entityManager = getEntityManagerForWebDB();
		entityManager.getTransaction().begin();
		RegisteredApp MapitApp = entityManager.createQuery("SELECT t FROM RegisteredApp t WHERE name=:name", RegisteredApp.class)
											.setParameter("name", "MAPIT")
											.getSingleResult();
		RegisteredApp MatchApp = entityManager.createQuery("SELECT t FROM RegisteredApp t WHERE name=:name", RegisteredApp.class)
											.setParameter("name", "match")
											.getSingleResult();
		MetaDataEntityDto metaDataEntityDto = new MetaDataEntityDto();
		metaDataEntityDto.setEmailUpdateUrl("/api/uus/email/update?privateAppId=[appId]&fromEmail=[email]&toEmail=[newEmail]");
		MapitApp.setMetaData(metaDataEntityDto);
		MatchApp.setMetaData(metaDataEntityDto);
		entityManager.getTransaction().commit();

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
