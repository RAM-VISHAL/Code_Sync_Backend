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
public class NotificationRequest {
	private Integer recipientId;
	private Integer senderId;
	private String senderName;
	private String senderEmail;
	private Long relatedId;
	private String message;
	private String type;
	private String projectName;
	private String recipientName;
}
