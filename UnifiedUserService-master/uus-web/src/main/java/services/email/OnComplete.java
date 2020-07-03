package services.email;

import javax.persistence.EntityManager;

public interface OnComplete {
	void onFail(EntityManager entityManager);

	void onSuccess(EntityManager entityManager);
}
