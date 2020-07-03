package dto;

import java.util.List;

public class BulkRegisterResponseDto {
	List<UserDto> inviteNRegister;
	List<RejectedUserDto> rejectedUsers;
	int numberOfCreatedUsers;
	int numberOfRejectedUsers;

	public List<UserDto> getInviteNRegister() {
		return this.inviteNRegister;
	}

	public void setInviteNRegister(List<UserDto> inviteNRegister) {
		this.inviteNRegister = inviteNRegister;
	}

	public List<RejectedUserDto> getRejectedUsers() {
		return this.rejectedUsers;
	}

	public void setRejectedUsers(List<RejectedUserDto> rejectedUsers) {
		this.rejectedUsers = rejectedUsers;
	}

	public int getNumberOfCreatedUsers() {
		return this.numberOfCreatedUsers;
	}

	public void setNumberOfCreatedUsers(int numberOfCreatedUsers) {
		this.numberOfCreatedUsers = numberOfCreatedUsers;
	}

	public int getNumberOfRejectedUsers() {
		return this.numberOfRejectedUsers;
	}

	public void setNumberOfRejectedUsers(int numberOfRejectedUsers) {
		this.numberOfRejectedUsers = numberOfRejectedUsers;
	}

}