package controllers;

import com.google.inject.Inject;

import ninja.Result;
import ninja.utils.NinjaProperties;

public class ControllerUtils {
	@Inject
	NinjaProperties ninjaProperties;

	
	Result resultWithCorsHeaders(Result result) {
		String accessControlAllowOrigin = ninjaProperties.get("Access-Control-Allow-Origin");
		return result.addHeader("Access-Control-Allow-Origin", accessControlAllowOrigin)
				.addHeader("Access-Control-Allow-Methods", "*")
				.addHeader("Access-Control-Allow-Headers",
						"Content-Type, Content-Range, Content-Disposition, Content-Description")
				.addHeader("Access-Control-Allow-Credentials", "true");
	}
	
}