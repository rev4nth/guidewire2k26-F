import axios from 'axios'

// Call the Spring Boot API directly (CORS enabled in backend). Override with VITE_API_URL if needed.
const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:2509'

const api = axios.create({
	baseURL,
	headers: {
		'Content-Type': 'application/json',
	},
})

api.interceptors.request.use((config) => {
	const token = localStorage.getItem('safeflex_token')
	if (token) {
		config.headers.Authorization = `Bearer ${token}`
	}
	return config
})

export default api
