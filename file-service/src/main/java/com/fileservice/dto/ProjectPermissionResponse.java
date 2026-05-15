package com.fileservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectPermissionResponse {
	private int projectId;
	private Integer userId;
	private boolean canView;
	private boolean canEdit;
	private boolean owner;
	private boolean member;
	private String visibility;
	private String membershipStatus;
	private String projectRole;
}
