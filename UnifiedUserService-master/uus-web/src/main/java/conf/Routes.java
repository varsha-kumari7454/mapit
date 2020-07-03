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


import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;
import controllers.ApplicationController;
import controllers.CacheClearanceListenerController;
import controllers.LoginRedirectControllerImpl;
import controllers.UUSAssetsController;
import controllers.AccessControlListControllerImpl;
import controllers.AdminControllerImpl;

public class Routes implements ApplicationRoutes {

    @Override
    public void init(Router router) {  
    	router.OPTIONS().route("/.*").with(ApplicationController::cors);

		router.GET().route("/dummy").with(ApplicationController::dummy);
        router.GET().route("/").with(ApplicationController::index);
        router.POST().route("/ajax/admin/login").with(AdminControllerImpl::isValidAdmin);
        router.GET().route("/ajax/admin/logout").with(AdminControllerImpl::logout);
    
    	
        router.POST().route("/ajax/user/login").with(LoginRedirectControllerImpl::loginUser);
        router.GET().route("/ajax/login/admin/email").with(LoginRedirectControllerImpl::getSecuredTokenByEmail);
        router.GET().route("/ajax/admin/emailUpdateHistory").with(AdminControllerImpl::getEmailUpdateHistory);
    
        
        //ACL routes    
    	router.GET().route("/ajax/get/acl/owner").with(AccessControlListControllerImpl::getACLByOwner);
    	router.GET().route("/ajax/get/acl/subUser").with(AccessControlListControllerImpl::getACLBySubUser);
    	router.POST().route("/ajax/add/acl").with(AccessControlListControllerImpl::addACL);
    	router.POST().route("/ajax/update/acl").with(AccessControlListControllerImpl::updateACL);
    	router.DELETE().route("/ajax/delete/acl").with(AccessControlListControllerImpl::deleteACL);
    	router.GET().route("/ajax/subuser/login").with(LoginRedirectControllerImpl::loginSubUser);
    	router.GET().route("/ajax/validate/subuser/action").with(LoginRedirectControllerImpl::validateSubUserActionWithToken);
    	router.GET().route("/ajax/subUserToken/refresh").with(LoginRedirectControllerImpl::refreshSubUserToken);

        //Register Users
        router.POST().route("/ajax/user/register").with(LoginRedirectControllerImpl::registerUser);
        router.POST().route("/ajax/user/register/activate").with(LoginRedirectControllerImpl::registerNActivateUser);
        router.POST().route("/ajax/user/register/admin").with(LoginRedirectControllerImpl::adminRegisteringUserWithoutPassword);
        router.POST().route("/ajax/user/register/bulk").with(LoginRedirectControllerImpl::registerUsersInBulk);
        router.POST().route("/ajax/user/register/invite").with(LoginRedirectControllerImpl::inviteNRegister);
        
        //http://localhost:8080?activationCodeHash=6c791b8be38efa6063444c366333f5696c15b6ddc5f556b0357c25adf1d0e168&email=nikhil.sadalagi@gmail.com&publicAppId=publicmapit&redirectLink=http://google.com/images
        router.GET().route("/ajax/user/activate").with(LoginRedirectControllerImpl::activateUser);
        router.GET().route("/ajax/user/exist").with(LoginRedirectControllerImpl::checkUserExist);
        
        router.GET().route("/ajax/user/password/forgot").with(LoginRedirectControllerImpl::forgotPassword);
        router.POST().route("/ajax/user/password/reset").with(LoginRedirectControllerImpl::resetPassword);
        router.POST().route("/ajax/user/email/reset").with(LoginRedirectControllerImpl::resetEmail);
        router.GET().route("/ajax/user/verify").with(LoginRedirectControllerImpl::verifyToken);
        router.GET().route("/ajax/user/logout").with(LoginRedirectControllerImpl::logout);
        
        router.POST().route("/ajax/user/password/update").with(LoginRedirectControllerImpl::updatePassword);
        
        router.POST().route("/ajax/emails/check/candidate").with(LoginRedirectControllerImpl::checkCandidateExistence);
        
        // Cache Clearance requests
        router.GET().route("/clearcache/{cacheClass}/{key}").with(CacheClearanceListenerController.class, "clearCache");
        router.GET().route("/clearcacheallservers/{cacheClass}/{key}").with(CacheClearanceListenerController.class, "clearCacheFromAllServers");
        router.GET().route("/cachestat").with(CacheClearanceListenerController.class, "getCacheStat");
        
        ///////////////////////////////////////////////////////////////////////
        // Assets (pictures / javascript)
        ///////////////////////////////////////////////////////////////////////    
        router.GET().route("/assets/webjars/{fileName: .*}").with(AssetsController::serveWebJars);
        router.GET().route("/assets/{fileName: .*}").with(AssetsController::serveWebJars);
        router.GET().route("/{fileName: .*}").with(UUSAssetsController::serveStatic);
    
        
        ///////////////////////////////////////////////////////////////////////
        // Index / Catchall shows index page
        ///////////////////////////////////////////////////////////////////////
        router.GET().route("/.*").with(ApplicationController::index);
        
    }

}
