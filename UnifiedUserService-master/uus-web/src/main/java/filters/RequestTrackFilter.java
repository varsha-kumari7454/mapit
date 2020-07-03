package filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import controllers.LoginRedirectControllerImpl;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;

public class RequestTrackFilter implements Filter{
	private static Logger log = LogManager.getLogger(LoginRedirectControllerImpl.class);
	
	@Override
	public Result filter(FilterChain filterChain,Context context){
		String controllerMethodName = context.getRoute().getControllerMethod().getName();
		log.info("UUS Request Track Info : "+context.getHeader("User-Agent")+"/"+context.getHeader("Referer")+"/"+controllerMethodName);
		Result result = filterChain.next(context);
		return result;
	}
}
