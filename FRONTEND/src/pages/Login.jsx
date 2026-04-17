import { useEffect, useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function redirectPathForRole(role) {
	switch (role) {
		case 'ADMIN':
			return '/admin'
		case 'WORKER':
			return '/worker'
		case 'GOVT':
			return '/govt'
		default:
			return '/login'
	}
}

function formatLoginError(data) {
	if (data == null) return 'Login failed. Try again.'
	if (typeof data === 'object' && data.error) return String(data.error)
	if (typeof data === 'string') {
		try {
			const parsed = JSON.parse(data)
			if (parsed?.error) return String(parsed.error)
		} catch {
			/* plain string */
		}
		return data
	}
	return 'Login failed. Try again.'
}

export default function Login() {
	const { login, token, user } = useAuth()
	const navigate = useNavigate()
	const location = useLocation()
	const [email, setEmail] = useState('')
	const [password, setPassword] = useState('')
	const [error, setError] = useState('')
	const [loading, setLoading] = useState(false)

	const from = location.state?.from?.pathname
	const registeredMsg = location.state?.registered

	useEffect(() => {
		if (!token || !user?.role) return
		const target =
			from && from !== '/login' ? from : redirectPathForRole(user.role)
		navigate(target, { replace: true })
	}, [token, user?.role, from, navigate])

	async function handleSubmit(e) {
		e.preventDefault()
		setError('')
		setLoading(true)
		try {
			const role = await login(email, password)
			const target =
				from && from !== '/login' ? from : redirectPathForRole(role)
			navigate(target, { replace: true })
		} catch (err) {
			const status = err.response?.status
			const data = err.response?.data
			const msg =
				status === 401
					? formatLoginError(data) || 'Invalid email or password'
					: formatLoginError(data)
			setError(msg)
		} finally {
			setLoading(false)
		}
	}

	return (
		<div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-100 via-white to-sky-50 px-4 py-12">
			<div className="w-full max-w-md rounded-2xl border border-slate-200/80 bg-white p-8 shadow-xl shadow-slate-200/50">
				<div className="mb-8 text-center">
					<h1 className="text-2xl font-bold tracking-tight text-slate-900">
						SafeFlex
					</h1>
					<p className="mt-1 text-sm text-slate-500">Sign in to continue</p>
				</div>

				<form onSubmit={handleSubmit} className="space-y-5">
					{registeredMsg ? (
						<div
							className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800"
							role="status"
						>
							{registeredMsg}
						</div>
					) : null}
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
							htmlFor="email"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Email
						</label>
						<input
							id="email"
							name="email"
							type="email"
							autoComplete="email"
							required
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							className="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2.5 text-slate-900 outline-none ring-sky-500/30 transition placeholder:text-slate-400 focus:border-sky-500 focus:bg-white focus:ring-4"
							placeholder="you@example.com"
						/>
					</div>

					<div>
						<label
							htmlFor="password"
							className="mb-1.5 block text-sm font-medium text-slate-700"
						>
							Password
						</label>
						<input
							id="password"
							name="password"
							type="password"
							autoComplete="current-password"
							required
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							className="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2.5 text-slate-900 outline-none ring-sky-500/30 transition placeholder:text-slate-400 focus:border-sky-500 focus:bg-white focus:ring-4"
							placeholder="••••••••"
						/>
					</div>

					<button
						type="submit"
						disabled={loading}
						className="w-full rounded-lg bg-sky-600 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60"
					>
						{loading ? 'Signing in…' : 'Login'}
					</button>

					<p className="text-center text-sm text-slate-500">
						Need an account?{' '}
						<Link to="/register" className="font-medium text-sky-700 hover:underline">
							Request access
						</Link>
					</p>
				</form>
			</div>
		</div>
	)
}
