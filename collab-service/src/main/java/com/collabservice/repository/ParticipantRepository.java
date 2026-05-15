package com.collabservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.collabservice.entity.Participant;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
	List<Participant> findBySessionId(String sessionId);

	Optional<Participant> findBySessionIdAndUserId(String sessionId, int userId);

	void deleteBySessionIdAndUserId(String sessionId, int userId);

	long countBySessionId(String sessionId);
}
