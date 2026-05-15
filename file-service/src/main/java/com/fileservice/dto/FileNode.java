package com.fileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a node in the project tree. Supports both
 * File and Folder types for hierarchical rendering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileNode {
	private String id; // Unique ID prefixed with type (e.g., "file-101")
	private String name;
	private String type; // "FILE" or "FOLDER"
	private String content; // Code content (null for folders)

	@Builder.Default
	private List<FileNode> children = new ArrayList<>();
}