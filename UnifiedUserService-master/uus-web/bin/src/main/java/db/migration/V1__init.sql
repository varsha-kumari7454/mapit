create table Admin (
	uuid int8 ,
	hashpassword varchar(255),
	username varchar(255),
	primary key (UUID)
);
create table RegisteredApp (
	UUID int8 ,
	appSecret varchar(255),
	logoURL varchar(255),
	name varchar(255),
	publicAppId varchar(255),
	primary key (UUID)
); 
create table RegisteredAppUser (
	UUID int8 not null,
	email varchar(255) UNIQUE NOT NULL,
	hashedPassword varchar(255),
	roles text,
	userDetails text,
	active boolean,
	registeredAt int8,
	modifiedAt int8,
	userPasswordUpdated boolean,
	passwordAsString varchar,
    	primary key (UUID)
);
create table RegisteredApp_RegisteredAppUser (
	registeredAppUsers_UUID int8 not null,
	registeredApps_UUID int8 not null
);
alter table RegisteredApp_RegisteredAppUser 
	add constraint FK_b14tfeq6pjus1urqlng4k0a42 
	foreign key (registeredApps_UUID) 
	references RegisteredApp;
alter table RegisteredApp_RegisteredAppUser 
	add constraint FK_2qeo6gaafb6jilyd358sm7p0e 
	foreign key (registeredAppUsers_UUID) 
	references RegisteredAppUser;
create sequence hibernate_sequence;

create table ValidUserSessionToken (
	id int8,
	uuid int8,
	expiryDate int8,
	token text,
	primary key (id)
);