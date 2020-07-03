package models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

import com.google.gson.Gson;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NamedQueries({ 
		@NamedQuery(name = "AccessControlList.findByOwnerId", query = "SELECT a FROM AccessControlList a WHERE a.owner.uuid = :ownerId"),
		@NamedQuery(name = "AccessControlList.findBySubUserId", query = "SELECT a FROM AccessControlList a WHERE a.subUser.uuid = :subUserId "),
		@NamedQuery(name = "AccessControlList.findByOwnerIdNdSubUserId", query = "SELECT a FROM AccessControlList a WHERE a.subUser.uuid = :subUserId AND a.owner.uuid = :ownerId")
})
@Table(name = "AccessControlList", uniqueConstraints = { @UniqueConstraint(name = "unique_acl_for_ownerNdSubUser", columnNames = { "owner_uuid", "subUser_uuid" }) })
public class AccessControlList {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@OneToOne(fetch=FetchType.EAGER)
	private RegisteredAppUser owner;
	
	@OneToOne(fetch=FetchType.EAGER)
	private RegisteredAppUser subUser;

	@NotNull(message = "Provide Atleast one Access Control to Subuser")
	@Type(type = "text")
	private String iACL; 
	
	private Long creationTime;
	private Long lastmodifiedTime;

	private static final Gson g = new Gson();
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	@PostPersist
	protected void onCreate() {
		creationTime = System.currentTimeMillis();
	}
	@PostUpdate
	protected void onUpdate() {
		lastmodifiedTime = System.currentTimeMillis();
	}
	public IndividualAccessControlListEntityDto getiACL() {
		return g.fromJson(this.iACL, IndividualAccessControlListEntityDto.class);
	}
	public void setiACL(IndividualAccessControlListEntityDto iACL) {
		this.iACL = g.toJson(iACL);
	}
	public Long getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Long creationTime) {
		this.creationTime = creationTime;
	}
	public Long getLastmodifiedTime() {
		return lastmodifiedTime;
	}
	public void setLastmodifiedTime(Long lastmodifiedTime) {
		this.lastmodifiedTime = lastmodifiedTime;
	}
	public RegisteredAppUser getOwner() {
		return owner;
	}
	public void setOwner(RegisteredAppUser owner) {
		this.owner = owner;
	}
	public RegisteredAppUser getSubUser() {
		return subUser;
	}
	public void setSubUser(RegisteredAppUser subUser) {
		this.subUser = subUser;
	}
	
}
