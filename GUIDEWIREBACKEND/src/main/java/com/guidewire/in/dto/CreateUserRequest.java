package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

	private String name;
	private String email;
	private String password;
	private String role;
	private String location;
	/** Optional Cloudinary HTTPS URL (set by admin) or leave null */
	private String profileImageUrl;
}
