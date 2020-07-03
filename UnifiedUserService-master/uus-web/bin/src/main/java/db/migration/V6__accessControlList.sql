CREATE TABLE accessControlList(
	id bigint,
	owner_uuid bigint,
	subuser_uuid bigint,
	iacl text,
	creationtime bigint,
	lastmodifiedtime bigint,
	CONSTRAINT accessControlList_pkey PRIMARY KEY (id),
	CONSTRAINT ownerId_fk FOREIGN KEY (owner_uuid) REFERENCES registeredappuser (uuid),
   	CONSTRAINT subUserId_fk FOREIGN KEY (subuser_uuid) REFERENCES registeredappuser (uuid),
   	CONSTRAINT accessControlList_unique UNIQUE (owner_uuid,subuser_uuid)
);