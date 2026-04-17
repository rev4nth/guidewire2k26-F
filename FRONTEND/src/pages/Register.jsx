import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'

const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:2509'

function parseMessage(data) {
	if (data == null) return null
	if (typeof data === 'object' && data.message) return String(data.message)
	if (typeof data === 'object' && data.error) return String(data.error)
	if (typeof data === 'string') {
		try {
			const p = JSON.parse(data)
			return p.message ?? p.error ?? data
		} catch {
			return data
		}
	}
	return null
}

export default function Register() {
	const navigate = useNavigate()
	const [name, setName] = useState('')
	const [email, setEmail] = useState('')
	const [password, setPassword] = useState('')
	const [location, setLocation] = useState('')
	const [role, setRole] = useState('WORKER')
	const [photo, setPhoto] = useState(null)
	const [error, setError] = useState('')
	const [loading, setLoading] = useState(false)

	async function handleSubmit(e) {
		e.preventDefault()
		setError('')
		setLoading(true)
		try {
			if (photo) {
				const fd = new FormData()
				fd.append('name', name)
				fd.append('email', email)
				fd.append('password', password)
				fd.append('role', role)
				if (location) fd.append('location', location)
				fd.append('photo', photo)
				await axios.post(`${baseURL}/auth/register`, fd)
			} else {
				await axios.post(`${baseURL}/auth/register`, {
					name,
					email,
					password,
					location: location || null,
					role,
				})
			}
			navigate('/login', {
				replace: true,
				state: {
					registered:
						'Your request was sent. An admin will verify it; once approved you can sign in.',
				},
			})
		} catch (err) {
			const msg = parseMessage(err.response?.data) || 'Registration failed.'
			setError(msg)
		} finally {
			setLoading(false)
		}
	}

	return (
		<div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-100 via-white to-emerald-50/40 px-4 py-12">
			<div className="w-full max-w-md rounded-2xl border border-slate-200/80 bg-white p-8 shadow-xl shadow-slate-200/50">
				<div className="mb-8 text-center">
					<h1 className="text-2xl font-bold tracking-tight text-slate-900">
						Request access
					</h1>
					<p className="mt-1 text-sm text-slate-500">
						Submit details for admin approval
					</p>
				</div>

				<form onSubmit={handleSubmit} className="space-y-5">
					{error ? (
						<div
							className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
							role="alert"
						>
							{error}
						</div>
					) : null}

					<div>
						<label
							htmlFor="reg-name"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Full name
						</label>
						<input
							id="reg-name"
							required
							value={name}
							onChange={(e) => setName(e.target.value)}
							className="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2.5 text-slate-900 outline-none ring-emerald-500/20 focus:border-emerald-500 focus:bg-white focus:ring-4"
						/>
					</div>

					<div>
						<label
							htmlFor="reg-email"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Email
						</label>
						<input
							id="reg-email"
							type="email"
							required
							autoComplete="email"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							className="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2.5 text-slate-900 outline-none ring-emerald-500/20 focus:border-emerald-500 focus:bg-white focus:ring-4"
						/>
					</div>

					<div>
						<label
							htmlFor="reg-password"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Password
						</label>
						<input
							id="reg-password"
							type="password"
							required
							autoComplete="new-password"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							className="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2.5 text-slate-900 outline-none ring-emerald-500/20 focus:border-emerald-500 focus:bg-white focus:ring-4"
						/>
					</div>

					<div>
						<label
							htmlFor="reg-role"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Role requested
						</label>
						<select
							id="reg-role"
							value={role}
							onChange={(e) => setRole(e.target.value)}
							className="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2.5 text-slate-900 outline-none focus:border-emerald-500 focus:bg-white focus:ring-4 focus:ring-emerald-500/20"
						>
							<option value="WORKER">Worker</option>
							<option value="GOVT">Government</option>
						</select>
					</div>

					<div>
						<label
							htmlFor="reg-location"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Location <span className="font-normal text-slate-400">(optional)</span>
						</label>
						<input
							id="reg-location"
							value={location}
							onChange={(e) => setLocation(e.target.value)}
							className="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2.5 text-slate-900 outline-none ring-emerald-500/20 focus:border-emerald-500 focus:bg-white focus:ring-4"
						/>
					</div>

					<div>
						<label
							htmlFor="reg-photo"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Profile photo{' '}
							<span className="font-normal text-slate-400">(optional, Cloudinary)</span>
						</label>
						<input
							id="reg-photo"
							type="file"
							accept="image/*"
							onChange={(e) => setPhoto(e.target.files?.[0] ?? null)}
							className="w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-slate-100 file:px-3 file:py-2 file:text-sm file:font-medium file:text-slate-800 hover:file:bg-slate-200"
						/>
					</div>

					<button
						type="submit"
						disabled={loading}
						className="w-full rounded-lg bg-emerald-600 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
					>
						{loading ? 'Submitting…' : 'Submit for approval'}
					</button>

					<p className="text-center text-sm text-slate-500">
						Already have an account?{' '}
						<Link to="/login" className="font-medium text-emerald-700 hover:underline">
							Sign in
						</Link>
					</p>
				</form>
			</div>
		</div>
	)
}
