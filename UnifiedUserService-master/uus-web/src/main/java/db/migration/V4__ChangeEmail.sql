create table EmailChangeTracker (
id bigint,
fromEmail varchar(255),
toEmail varchar(255),
initTime int8,
emailChecked boolean,
status text,
appStatus text,
comment text,
ip varchar(255),
primary key (id)
);
 
ALTER TABLE RegisteredApp
ADD metaData text;