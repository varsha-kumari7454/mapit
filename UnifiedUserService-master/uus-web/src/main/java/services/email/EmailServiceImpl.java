package services.email;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;

import ninja.postoffice.Mail;
import ninja.postoffice.Postoffice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class EmailServiceImpl implements EmailService {
	@Inject
	Provider<EntityManager> entityManagerProvider;
	@Inject
	private Postoffice postoffice;
	@Inject
	private Provider<MailSendTask> mailSendTaskProvider;
	private ExecutorService exService = Executors.newFixedThreadPool(2);

	@Override
	public void sendAsyncMail(final List<MailSendTask> emailToSend, final long startSendingDelay) {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(startSendingDelay);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				sendAsyncMail(emailToSend);

			}
		});
		t.start();
	}

	@Override
	public void sendAsyncMail(List<MailSendTask> emailToSend) {
		for (MailSendTask mailSendTask : emailToSend) {
			mailSendTask.setPostOffice(postoffice);
			exService.submit(mailSendTask);
		}
	}

	@Override
	public void sendSyncMail(Mail mail) throws Exception {
		postoffice.send(mail);
	}

	@Override
	public void sendAsyncMail(Mail mail) throws Exception {
		MailSendTask mailSendTask = mailSendTaskProvider.get();
		mailSendTask.SetMail(mail);
		mailSendTask.setInjectedSelf(mailSendTask);
		List<MailSendTask> taskList = new ArrayList<>();
		taskList.add(mailSendTask);
		sendAsyncMail(taskList);

	}
}
