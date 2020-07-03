package controllers;

import ninja.Context;
import ninja.Result;

public interface AdminController {
	ninja.Result isValidAdmin(String username, String password, Context context);

	ninja.Result getEmailUpdateHistory(Context context);

	ninja.Result logout(Context context);
}
