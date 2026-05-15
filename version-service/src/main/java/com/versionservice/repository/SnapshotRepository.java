package com.versionservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.versionservice.entity.Snapshot;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
	List<Snapshot> findByFileIdOrderByCreatedAtDesc(Integer fileId);

	Optional<Snapshot> findFirstByFileIdOrderByCreatedAtDesc(Integer fileId);
}