package com.projectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
