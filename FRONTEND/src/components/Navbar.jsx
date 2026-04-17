import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
	const { user, logout } = useAuth()
	const navigate = useNavigate()

	function handleLogout() {
		logout()
		navigate('/login', { replace: true })
	}

	return (
		<header className="border-b border-slate-200/80 bg-white/90 backdrop-blur-sm">
			<div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
				<span className="text-sm font-semibold tracking-tight text-slate-800">
					SafeFlex
				</span>
				<div className="flex items-center gap-3">
					{user?.profileImageUrl ? (
						<img
							src={user.profileImageUrl}
							alt=""
							className="h-9 w-9 rounded-full border border-slate-200 object-cover"
						/>
					) : null}
					<span className="hidden max-w-[10rem] truncate text-sm text-slate-600 sm:inline">
						{user?.name ?? user?.email ?? ''}
					</span>
					<span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
						{user?.role ?? '—'}
					</span>
					<button
						type="button"
						onClick={handleLogout}
						className="rounded-lg bg-slate-900 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-slate-800"
					>
						Logout
					</button>
				</div>
			</div>
		</header>
	)
}
