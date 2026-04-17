import { useCallback, useEffect, useRef, useState } from 'react'
import api from '../api/api'

const OPENWEATHER_KEY = import.meta.env.VITE_OPENWEATHER_KEY ?? ''

/**
 * useGeoLocation
 *
 * - Requests browser geolocation on mount (once) or when `request()` is called.
 * - Reverse-geocodes to a city name via OpenWeather (or Nominatim as fallback).
 * - Sends { latitude, longitude, city } to POST /worker/location.
 * - Returns { coords, city, status, error, request }
 *
 * `status`: 'idle' | 'requesting' | 'syncing' | 'ok' | 'error'
 */
export function useGeoLocation({ autoRequest = true } = {}) {
	const [coords, setCoords]   = useState(null)   // { lat, lon }
	const [city, setCity]       = useState(null)
	const [status, setStatus]   = useState('idle')
	const [error, setError]     = useState(null)
	const hasFetched = useRef(false)

	const reverseGeocode = useCallback(async (lat, lon) => {
		// Primary: OpenWeather reverse geocode (free tier, no key needed for basic city lookup)
		try {
			const url = `https://api.openweathermap.org/geo/1.0/reverse?lat=${lat}&lon=${lon}&limit=1&appid=${OPENWEATHER_KEY}`
			if (OPENWEATHER_KEY) {
				const res  = await fetch(url)
				const data = await res.json()
				if (Array.isArray(data) && data.length > 0) {
					return data[0].name   // e.g. "Hyderabad"
				}
			}
		} catch { /* fall through */ }

		// Fallback: Nominatim (no key required)
		try {
			const res  = await fetch(
				`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`,
				{ headers: { 'Accept-Language': 'en' } }
			)
			const data = await res.json()
			return (
				data?.address?.city    ??
				data?.address?.town    ??
				data?.address?.village ??
				data?.address?.county  ??
				null
			)
		} catch { /* ignore */ }

		return null
	}, [])

	const request = useCallback(async () => {
		if (!navigator.geolocation) {
			setStatus('error')
			setError('Geolocation is not supported by this browser.')
			return
		}
		setStatus('requesting')
		setError(null)

		navigator.geolocation.getCurrentPosition(
			async (pos) => {
				const lat = pos.coords.latitude
				const lon = pos.coords.longitude
				setCoords({ lat, lon })

				setStatus('syncing')
				const resolvedCity = await reverseGeocode(lat, lon)
				setCity(resolvedCity)

				// POST to backend
				try {
					await api.post('/worker/location', {
						latitude:  lat,
						longitude: lon,
						city:      resolvedCity ?? undefined,
					})
					setStatus('ok')
				} catch (err) {
					// Non-critical — still show coords even if backend save fails
					setStatus('error')
					setError('Location saved locally but failed to sync with server.')
				}
			},
			(err) => {
				setStatus('error')
				const messages = {
					1: 'Location access denied. Please allow location permission in your browser.',
					2: 'Location unavailable. Check your device settings.',
					3: 'Location request timed out.',
				}
				setError(messages[err.code] ?? 'Failed to get location.')
			},
			{ enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
		)
	}, [reverseGeocode])

	useEffect(() => {
		if (autoRequest && !hasFetched.current) {
			hasFetched.current = true
			request()
		}
	}, [autoRequest, request])

	return { coords, city, status, error, request }
}
