package dto;

import java.util.List;

public class BulkUserDto {

	List<UserDto> usersDto;

	public List<UserDto> getUsersDto() {
		return usersDto;
	}

	public void setUsersDto(List<UserDto> usersDto) {
		this.usersDto = usersDto;
	}
}
