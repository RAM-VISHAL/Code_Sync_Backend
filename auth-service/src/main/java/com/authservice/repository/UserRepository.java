package com.authservice.repository;

import com.authservice.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByEmail(String email); // find by email

	Optional<User> findByUsername(String username); // find by username

	boolean existsByEmail(String email); // find if email exists 

	boolean existsByUsername(String username); // find if username exists

	List<User> findAllByRole(String role); // find by role all users 

	List<User> searchByUsername(String query); // search by username
	
	
}