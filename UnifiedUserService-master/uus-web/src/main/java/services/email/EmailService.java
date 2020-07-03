package services.email;

import java.util.List;

import ninja.postoffice.Mail;

public interface EmailService {
	void sendAsyncMail(Mail mail) throws Exception;

	void sendAsyncMail(List<MailSendTask> emailToSend);

	void sendSyncMail(Mail mail) throws Exception;

	void sendAsyncMail(List<MailSendTask> emailToSend, long startSendingDelay);
}
