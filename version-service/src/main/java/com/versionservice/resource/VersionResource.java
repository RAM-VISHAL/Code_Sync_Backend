package com.versionservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.versionservice.entity.Snapshot;
import com.versionservice.service.VersionService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/versions")
public class VersionResource {

	@Autowired
	private VersionService versionService;

	@PostMapping("/snapshot")
	public ResponseEntity<Snapshot> takeSnapshot(@RequestBody Snapshot snapshot) {
		// Manually triggered by the "Save/Commit" button in the Editor UI
		return ResponseEntity.ok(versionService.createSnapshot(snapshot));
	}

	@GetMapping("/history/{fileId}")
	public ResponseEntity<List<Snapshot>> getFileHistory(@PathVariable Integer fileId) {
		return ResponseEntity.ok(versionService.getFileHistory(fileId));
	}

	@GetMapping("/diff")
	public ResponseEntity<Map<String, Object>> getLineDiff(@RequestParam Long base, @RequestParam Long head) {
		// Used for the "Compare Versions" side-by-side view
		return ResponseEntity.ok(versionService.compareSnapshots(base, head));
	}

	@PostMapping("/restore/{snapshotId}")
	public ResponseEntity<Snapshot> revertToFileVersion(@PathVariable Long snapshotId, @RequestParam Integer userId) {
		return ResponseEntity.ok(versionService.restoreVersion(snapshotId, userId));
	}
}