package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostPersist;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import com.google.gson.Gson;

@Entity
@Table(name = "EmailChangeTracker")
@NamedQueries({
		@NamedQuery(name = "EmailChangeTracker.findByEmail", query = "SELECT u FROM EmailChangeTracker u WHERE u.fromEmail=:fromEmail "),
		@NamedQuery(name = "EmailChangeTracker.findById", query = "SELECT u FROM EmailChangeTracker u WHERE u.id=:id ")})
public class EmailChangeTracker {

	private static final long serialVersionUID = 1L;
	@Id	
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String fromEmail;
	private String toEmail;
	private Long initTime;
	private boolean emailChecked;
	private String comment;
	private String ip;
	
	
	@Enumerated(EnumType.STRING)
	private EmailUpdateStatus status;
	
	@Type(type = "text")
	private String appStatus;

	@PostPersist
	public void updateInitTime() {
		this.initTime = System.currentTimeMillis();
	}
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	public Long getInitTime() {
		return initTime;
	}

	public void setInitTime(Long initTime) {
		this.initTime = initTime;
	}

	public Boolean getEmailChecked() {
		return emailChecked;
	}
	
	public void setEmailChecked(boolean emailChacked) {
		this.emailChecked = emailChacked;
	}

	private static final Gson g = new Gson();

	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String email) {
		this.fromEmail = email;
	}
	public String getToEmail() {
		return toEmail;
	}

	public void setToEmail(String email) {
		this.toEmail = email;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public EmailUpdateStatus getStatus() {
		return status;
	}

	public void setStatus(EmailUpdateStatus status) {
		this.status = status;
	}
	
	public AppStatusEntityDto getAppStatus() {
		return g.fromJson(this.appStatus, AppStatusEntityDto.class);
	}

	public void setAppStatus(AppStatusEntityDto AppStatus) {
		this.appStatus = g.toJson(AppStatus);
	}
	

}
