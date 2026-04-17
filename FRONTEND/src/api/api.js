import axios from 'axios'

// Call the Spring Boot API directly (CORS enabled in backend). Override with VITE_API_URL if needed.
const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:2509'

const api = axios.create({
	baseURL,
	headers: {
		'Content-Type': 'application/json',
	},
})

/** Unwrap `{ status: "SUCCESS", message, data }` from backend; leave other bodies unchanged (e.g. /auth/login). */
function unwrapSuccess(body) {
	if (body && typeof body === 'object' && body.status === 'SUCCESS' && Object.prototype.hasOwnProperty.call(body, 'data')) {
		return body.data
	}
	return body
}

api.interceptors.response.use(
	(response) => {
		response.data = unwrapSuccess(response.data)
		return response
	},
	(error) => {
		const d = error.response?.data
		if (d && typeof d === 'object' && d.status === 'FAILED' && d.error) {
			error.safeflexMessage = d.error
		}
		return Promise.reject(error)
	},
)

api.interceptors.request.use((config) => {
	const token = localStorage.getItem('safeflex_token')
	if (token) {
		config.headers.Authorization = `Bearer ${token}`
	}
	return config
})

/** Multipart claim proof — avoids forcing JSON Content-Type on FormData. */
export async function uploadClaimProof(claimId, file) {
	const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:2509'
	const token = localStorage.getItem('safeflex_token')
	const fd = new FormData()
	fd.append('claimId', String(claimId))
	fd.append('image', file)
	const res = await fetch(`${baseURL}/worker/upload-proof`, {
		method: 'POST',
		headers: token ? { Authorization: `Bearer ${token}` } : {},
		body: fd,
	})
	const raw = await res.json().catch(() => ({}))
	const data = unwrapSuccess(raw)
	if (!res.ok) {
		const msg =
			raw?.status === 'FAILED' && raw?.error
				? raw.error
				: typeof raw?.error === 'string'
					? raw.error
					: typeof data?.error === 'string'
						? data.error
						: 'Upload failed'
		throw new Error(msg)
	}
	return data
}

export default api
