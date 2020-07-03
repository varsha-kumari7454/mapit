package models;

public enum AccessPermission {
	/**
	 * ADMIN : can view,modify and publish
	 * VIEW : only view
	 * MODIFY : can view, modify
	 * PUBLISH : can view , modify, publish
	 * REMOVE : can view, remove
	 */
	ADMIN,MODIFY,VIEW,PUBLISH,REMOVE
}
