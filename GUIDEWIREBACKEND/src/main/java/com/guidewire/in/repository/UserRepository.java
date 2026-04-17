package com.guidewire.in.repository;

import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	List<User> findByRole(Role role);

	List<User> findByRoleAndLocationIgnoreCase(Role role, String location);

	/**
	 * Find all workers whose location string matches (case-insensitive).
	 * Used for location-based disruption broadcast.
	 */
	@Query("SELECT u FROM User u WHERE u.role = :role AND LOWER(u.location) = LOWER(:location)")
	List<User> findWorkersByLocation(@Param("role") Role role, @Param("location") String location);

	/**
	 * Find workers that have GPS coordinates set.
	 */
	@Query("SELECT u FROM User u WHERE u.role = :role AND u.latitude IS NOT NULL AND u.longitude IS NOT NULL")
	List<User> findWorkersWithCoordinates(@Param("role") Role role);
}
