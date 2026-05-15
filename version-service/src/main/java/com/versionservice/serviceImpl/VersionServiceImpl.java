package com.versionservice.serviceImpl;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.versionservice.entity.Snapshot;
import com.versionservice.repository.SnapshotRepository;
import com.versionservice.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VersionServiceImpl implements VersionService {

	@Autowired
	private SnapshotRepository repository;

	@Override
	@Transactional
	public Snapshot createSnapshot(Snapshot snapshot) {
		// 1. Calculate SHA-256 for integrity verification
		snapshot.setHash(calculateHash(snapshot.getContent()));

		// 2. Automatically link to the latest version of this file
		repository.findFirstByFileIdOrderByCreatedAtDesc(snapshot.getFileId())
				.ifPresent(latest -> snapshot.setParentSnapshotId(latest.getId()));

		return repository.save(snapshot);
	}

	@Override
	public Map<String, Object> compareSnapshots(Long baseId, Long headId) {
		Snapshot base = repository.findById(baseId)
				.orElseThrow(() -> new RuntimeException("Diff Error: Base version not found"));
		Snapshot head = repository.findById(headId)
				.orElseThrow(() -> new RuntimeException("Diff Error: Head version not found"));

		List<String> baseLines = Arrays.asList(base.getContent().split("\n"));
		List<String> headLines = Arrays.asList(head.getContent().split("\n"));

		// Generate line-by-line diff using Myers Algorithm
		Patch<String> patch = DiffUtils.diff(baseLines, headLines);
		List<String> changes = patch.getDeltas().stream().map(AbstractDelta::toString).collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("baseVersion", base.getId());
		response.put("headVersion", head.getId());
		response.put("diffs", changes);
		return response;
	}

	@Override
	@Transactional
	public Snapshot restoreVersion(Long snapshotId, Integer userId) {
		Snapshot target = repository.findById(snapshotId)
				.orElseThrow(() -> new RuntimeException("Restore Failed: Version ID not found"));

		// Industry Standard: Restore by creating a NEW snapshot with the OLD content
		// This preserves the history of the restoration itself.
		Snapshot restoredSnapshot = Snapshot.builder().fileId(target.getFileId()).content(target.getContent())
				.commitMessage("System: Restored to version #" + snapshotId).userId(userId).build();

		return createSnapshot(restoredSnapshot);
	}

	@Override
	public List<Snapshot> getFileHistory(Integer fileId) {
		return repository.findByFileIdOrderByCreatedAtDesc(fileId);
	}

	private String calculateHash(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(content.getBytes("UTF-8"));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			return "HASH_ERROR_" + UUID.randomUUID().toString().substring(0, 8);
		}
	}

	@Override
	public Snapshot getLatestSnapshot(Integer fid) {
		return repository.findFirstByFileIdOrderByCreatedAtDesc(fid).orElse(null);
	}
}