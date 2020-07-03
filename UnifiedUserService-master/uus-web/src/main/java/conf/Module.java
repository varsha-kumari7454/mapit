/**
 * Copyright (C) 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package conf;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import caches.ACLCache;
import caches.ACLCacheGuava;
import caches.CacheInvalidator;
import caches.CacheInvalidatorImpl;
import caches.RegisteredAppCache;
import caches.RegisteredAppCacheGuava;
import caches.RegisteredAppUserCache;
import caches.RegisteredAppUserCacheGuava;
import controllers.AccessControlListController;
import controllers.AccessControlListControllerImpl;
import controllers.ControllerUtils;
import controllers.LoginRedirectController;
import controllers.LoginRedirectControllerImpl;
import dao.SetupDao;
import dto.Pair;
import facade.AppFacade;
import facade.AppFacadeImpl;
import facade.TokenFacade;
import facade.TokenFacadeImpl;
import facade.UserFacade;
import facade.UserFacadeImpl;
import facade.AdminFacadeImpl;
import facade.ACLFacade;
import facade.ACLFacadeImpl;
import facade.AdminFacade;
import ninja.postoffice.NinjaPostofficeModule;
import services.email.EmailService;
import services.email.EmailServiceImpl;


@Singleton
public class Module extends AbstractModule {
    
	@Override
    protected void configure() {
        
    	/**
    	 * These actions take place at priority given in the @start
    	 * we use it
    	 * 		to initialize the data(dummy data for development)
    	 * 		to control the migrations,if we want to update the schema
    	 */
    	bind(StartupActions.class);
    	bind(SetupDao.class);

		bind(UserFacade.class).to(UserFacadeImpl.class).in(Singleton.class);
		bind(AdminFacade.class).to(AdminFacadeImpl.class).in(Singleton.class);
		bind(AppFacade.class).to(AppFacadeImpl.class).in(Singleton.class);
		bind(TokenFacade.class).to(TokenFacadeImpl.class).in(Singleton.class);
		bind(ACLFacade.class).to(ACLFacadeImpl.class).in(Singleton.class);
        bind(ControllerUtils.class).in(Singleton.class);
        bind(LoginRedirectController.class).to(LoginRedirectControllerImpl.class).in(Singleton.class);
        bind(AccessControlListController.class).to(AccessControlListControllerImpl.class).in(Singleton.class);
        bind(RegisteredAppUserCache.class).to(RegisteredAppUserCacheGuava.class).in(Singleton.class);
        bind(RegisteredAppCache.class).to(RegisteredAppCacheGuava.class).in(Singleton.class);
        bind(ACLCache.class).to(ACLCacheGuava.class).in(Singleton.class);
        bind(new TypeLiteral<LinkedBlockingQueue<Pair>>() {}).annotatedWith(Names.named("cacheInvalidateQueue")).toInstance(new LinkedBlockingQueue<Pair>());
        
        bind(CacheInvalidator.class).to(CacheInvalidatorImpl.class).in(Singleton.class);
        bind(CloseableHttpClient.class).toInstance(createHttpClient());

        bind(EmailService.class).to(EmailServiceImpl.class).in(Singleton.class);
        
        install(new NinjaPostofficeModule());
    }

	private CloseableHttpClient createHttpClient() {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(100);
		cm.setDefaultMaxPerRoute(100);
		return HttpClients.custom().setConnectionManager(cm).build();
	}
}
