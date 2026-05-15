package com.projectservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSummaryResponse {
	private Integer userId;
	private String username;
	private String email;
	private String role;
}
