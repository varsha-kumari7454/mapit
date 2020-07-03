package facade;

import java.io.IOException;
import dto.AdminUsersDto;
import models.EmailChangeTracker;
import java.util.List;
public interface AdminFacade {

	AdminUsersDto getAdminUsers() throws IOException;
	List<EmailChangeTracker> getEmailUpdateHistory() throws IOException;
}
