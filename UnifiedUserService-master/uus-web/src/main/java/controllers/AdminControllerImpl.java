package controllers;

import java.util.ArrayList;

import java.util.List;

import javax.persistence.EntityManager;
import java.io.IOException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import ninja.Context;
import ninja.FilterWith;
import ninja.session.*;
import dto.AdminUserDto;
import dto.AdminUsersDto;
import facade.AdminFacade;
import facade.UserFacadeImpl;
import models.EmailChangeTracker;
import ninja.Result;
import ninja.Results;
import ninja.params.Param;

public class AdminControllerImpl implements AdminController {
	private static final Logger log = LogManager.getLogger(UserFacadeImpl.class.getName());
	@Inject
	AdminFacade adminFacade;
	@Inject
	private ControllerUtils controllerUtils;
	@Inject
	Context context;
	@Inject
	private Provider<EntityManager> entityManagerProvider;

	@Override
	public Result isValidAdmin(@Param("email") String username, @Param("password") String password, Context context) {
		log.info("isValidAdmin called" + username + " || " + password);
		try {
			AdminUsersDto adminUsersDto = new AdminUsersDto();
			adminUsersDto = adminFacade.getAdminUsers();
			List<AdminUserDto> admins = new ArrayList<AdminUserDto>();
			admins = adminUsersDto.getUsers();
			for (int i = 0; i < admins.size(); i++) {
				if (admins.get(i).getUserName().equals(username) && admins.get(i).getPassword().equals(password)) {
					log.info("valid admin");
					context.getSession().put("username", username);
					context.getSession().setExpiryTime(60 * 60 * 1000L);
					return controllerUtils.resultWithCorsHeaders(Results.ok().json().render("Login Successful"));
				}
			}
			log.info("not valid admin");
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("user does not exist"));
		} catch (IOException e) {
			log.info(e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render("error in getting the admin from json"));
		} catch (Exception e) {
			log.info(e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e));
		}
	}

	@Override
	public Result getEmailUpdateHistory(Context context) {
		log.info("getEmailUpdateHistory called");
		try {
			if (context.getSession().get("username") != null) {
				List<EmailChangeTracker> emailChangeTracker = adminFacade.getEmailUpdateHistory();
				return controllerUtils.resultWithCorsHeaders(Results.ok().json().render(emailChangeTracker));
			}
		} catch (IOException e) {
			log.info(e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e));
		} catch (Exception e) {
			log.info(e);
			e.printStackTrace();
			return controllerUtils.resultWithCorsHeaders(Results.badRequest().json().render(e));
		}
		return controllerUtils.resultWithCorsHeaders(Results.unauthorized().json().render("Please login"));
	}

	@Override
	public Result logout(Context context) {
		log.info("logout called");
		context.getSession().clear();
		return controllerUtils.resultWithCorsHeaders(Results.ok().json().render("logout Successful"));
	}
}
