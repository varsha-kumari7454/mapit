package services.email;

import java.util.concurrent.Callable;

import javax.persistence.EntityManager;

import ninja.postoffice.Mail;
import ninja.postoffice.Postoffice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

public class MailSendTask implements Callable<String> {
	private static Logger log = LogManager.getLogger(MailSendTask.class);
	private Mail mail;
	private Postoffice postoffice;
	private MailSendTask injectedSelf;
	private OnComplete onComplete;
	@Inject
	Provider<EntityManager> entitiyManagerProvider;
	protected Status status = Status.NOTTRIED;

	public enum Status {
		PASS, FAIL, NOTTRIED
	};

	public OnComplete getOnComplete() {
		return onComplete;
	}

	public void setOnComplete(OnComplete onComplete) {
		this.onComplete = onComplete;
	}

	public void setInjectedSelf(MailSendTask self) {
		this.injectedSelf = self;
	}

	public Mail getMail() {
		return mail;
	}

	public void SetMail(Mail mail) {
		this.mail = mail;
	}

	@Override
	public String call() throws Exception {
		return injectedSelf.work();
	}

	public String work() {
		log.debug("sending mail");
		try {
			postoffice.send(mail);
		} catch (Exception e) {
			log.error("error", e);
			if (onComplete != null) {
				injectedSelf.onFailTransactional();
			}
			return "fail";
		}
		if (onComplete != null) {
			injectedSelf.onSuccessTransactional();
		}
		return "success";
	}

	@Transactional
	public void onSuccessTransactional() {
		EntityManager entityManager = entitiyManagerProvider.get();
		double t = System.currentTimeMillis();
		try {
			onComplete.onSuccess(entityManager);
			status = Status.PASS;
		} catch (Exception e) {
			log.error(e);
		}
		System.out.println("********************" + (System.currentTimeMillis() - t) / 1000);
	}

	@Transactional
	public void onFailTransactional() {
		EntityManager entityManager = entitiyManagerProvider.get();
		try {
			onComplete.onFail(entityManager);
			status = Status.FAIL;
		} catch (Exception e1) {
			log.error(e1);
		}
	}

	protected void setPostOffice(Postoffice postoffice) {
		this.postoffice = postoffice;
	}
}
