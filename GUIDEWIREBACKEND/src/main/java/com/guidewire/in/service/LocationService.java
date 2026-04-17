package com.guidewire.in.service;

import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

	private static final double EARTH_RADIUS_KM = 6371.0;

	private final UserRepository userRepository;

	public LocationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Haversine formula — returns distance in kilometres between two GPS points.
	 */
	public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_KM * c;
	}

	/**
	 * Find the nearest WORKER to a reference point.
	 * Falls back to workers with a matching location string if no GPS coords available.
	 */
	public Optional<User> findNearestWorker(double refLat, double refLon) {
		List<User> workers = userRepository.findWorkersWithCoordinates(Role.WORKER);
		return workers.stream()
				.min(Comparator.comparingDouble(w -> distanceKm(refLat, refLon, w.getLatitude(), w.getLongitude())));
	}

	/**
	 * Find the nearest WORKER to a reference point within a max radius (km).
	 */
	public Optional<User> findNearestWorkerWithinRadius(double refLat, double refLon, double maxKm) {
		List<User> workers = userRepository.findWorkersWithCoordinates(Role.WORKER);
		return workers.stream()
				.filter(w -> distanceKm(refLat, refLon, w.getLatitude(), w.getLongitude()) <= maxKm)
				.min(Comparator.comparingDouble(w -> distanceKm(refLat, refLon, w.getLatitude(), w.getLongitude())));
	}

	/**
	 * Find all WORKERs within radius km of a reference point.
	 */
	public List<User> findWorkersNearby(double refLat, double refLon, double radiusKm) {
		List<User> workers = userRepository.findWorkersWithCoordinates(Role.WORKER);
		return workers.stream()
				.filter(w -> distanceKm(refLat, refLon, w.getLatitude(), w.getLongitude()) <= radiusKm)
				.sorted(Comparator.comparingDouble(w -> distanceKm(refLat, refLon, w.getLatitude(), w.getLongitude())))
				.toList();
	}

	/**
	 * Find all WORKERs in the same city (location string).
	 */
	public List<User> findWorkersByCity(String city) {
		return userRepository.findWorkersByLocation(Role.WORKER, city);
	}
}
