package com.versionservice.service;

import java.util.List;
import java.util.Map;

import com.versionservice.entity.Snapshot;

public interface VersionService {
	Snapshot createSnapshot(Snapshot snapshot);

	List<Snapshot> getFileHistory(Integer fileId);

	Snapshot getLatestSnapshot(Integer fileId);

	Map<String, Object> compareSnapshots(Long baseId, Long headId);

	Snapshot restoreVersion(Long snapshotId, Integer userId);
}