import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/* ─── icons ─── */
function IcDashboard({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" />
		</svg>
	)
}
function IcUsers({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
		</svg>
	)
}
function IcPolicies({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25zM6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z" />
		</svg>
	)
}
function IcFinance({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M2.25 18.005h19.5M3.75 6.005v12h16.5v-12M3.75 6.005c0-1.243 1.007-2.25 2.25-2.25h13.5c1.243 0 2.25 1.007 2.25 2.25v0c0 1.243-1.007 2.25-2.25 2.25H6c-1.243 0-2.25-1.007-2.25-2.25v0zm4.5 0v.75m7.5-.75v.75" />
		</svg>
	)
}
function IcDisruptions({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
		</svg>
	)
}
function IcProfile({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
		</svg>
	)
}
function IcChevron() {
	return (
		<svg className="h-4 w-4 text-gray-600" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
		</svg>
	)
}

const NAV = [
	{ id: 'dashboard', label: 'Dashboard', Icon: IcDashboard },
	{ id: 'users',     label: 'Users',     Icon: IcUsers },
	{ id: 'policies',  label: 'Policies',  Icon: IcPolicies },
	{ id: 'finance',   label: 'Finance',   Icon: IcFinance },
	{ id: 'disruptions', label: 'Disruptions', Icon: IcDisruptions },
	{ id: 'profile',   label: 'My Profile', Icon: IcProfile },
]

function Avatar({ src, name, size = 9 }) {
	const dim = `h-${size} w-${size}`
	const initials = (name ?? '?')[0].toUpperCase()
	if (src) return <img src={src} alt={name} className={`${dim} rounded-full border border-gray-700 object-cover`} />
	return (
		<div className={`${dim} flex items-center justify-center rounded-full bg-indigo-500 text-xs font-bold text-white`}>
			{initials}
		</div>
	)
}

export default function AdminShell({ activeTab, onTabChange, children }) {
	const { user, logout } = useAuth()
	const navigate = useNavigate()
	const [sidebarOpen, setSidebarOpen] = useState(true)

	function handleLogout() {
		logout()
		navigate('/login', { replace: true })
	}

	return (
		<div className="flex h-screen overflow-hidden bg-gray-100">

			{/* ══════════════ SIDEBAR ══════════════ */}
			<aside
				className={`flex flex-col bg-gray-900 transition-all duration-300 ease-in-out ${
					sidebarOpen ? 'w-60' : 'w-16'
				} flex-shrink-0`}
			>
				{/* logo row */}
				<div className="flex h-16 items-center justify-between border-b border-gray-800 px-4">
					{sidebarOpen && (
						<div className="flex items-center gap-2">
							<div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-500">
								<svg className="h-4 w-4 text-white" fill="currentColor" viewBox="0 0 20 20">
									<path d="M9 4.804A7.968 7.968 0 005.5 4c-1.255 0-2.443.29-3.5.804v10A7.969 7.969 0 015.5 14c1.669 0 3.218.51 4.5 1.385A7.962 7.962 0 0114.5 14c1.255 0 2.443.29 3.5.804v-10A7.968 7.968 0 0014.5 4c-1.255 0-2.443.29-3.5.804V12a1 1 0 11-2 0V4.804z" />
								</svg>
							</div>
							<span className="text-base font-extrabold tracking-tight text-white">SafeFlex</span>
						</div>
					)}
					<button
						type="button"
						onClick={() => setSidebarOpen((s) => !s)}
						className="rounded-md p-1.5 text-gray-400 hover:bg-gray-800 hover:text-white"
						aria-label="Toggle sidebar"
					>
						<IcChevron />
					</button>
				</div>

				{/* nav items */}
				<nav className="flex-1 space-y-1 overflow-y-auto px-2 py-4">
					{NAV.map(({ id, label, Icon }) => {
						const active = activeTab === id
						return (
							<button
								key={id}
								type="button"
								onClick={() => onTabChange(id)}
								className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150 ${
									active
										? 'bg-gray-700 text-white shadow-sm'
										: 'text-gray-400 hover:bg-gray-800 hover:text-white'
								}`}
							>
								<Icon active={active} />
								{sidebarOpen && <span className="truncate">{label}</span>}
								{sidebarOpen && id === 'users' && (
									<span className="ml-auto rounded-full bg-blue-500 px-1.5 py-0.5 text-[10px] font-bold text-white">
										{String(user?.totalUsers ?? '')}
									</span>
								)}
							</button>
						)
					})}
				</nav>

				{/* user row */}
				<div className="border-t border-gray-800 p-3">
					{sidebarOpen ? (
						<div className="flex items-center gap-3">
							<Avatar src={user?.profileImageUrl} name={user?.name ?? 'A'} size={8} />
							<div className="min-w-0 flex-1">
								<p className="truncate text-sm font-semibold text-white">{user?.name ?? 'Admin'}</p>
								<p className="truncate text-xs text-gray-400">{user?.email ?? ''}</p>
							</div>
							<button
								type="button"
								onClick={handleLogout}
								title="Logout"
								className="rounded-md p-1.5 text-gray-400 hover:bg-gray-800 hover:text-red-400"
							>
								<svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
								</svg>
							</button>
						</div>
					) : (
						<div className="flex flex-col items-center gap-2">
							<Avatar src={user?.profileImageUrl} name={user?.name ?? 'A'} size={8} />
							<button type="button" onClick={handleLogout} title="Logout" className="rounded-md p-1 text-gray-400 hover:text-red-400">
								<svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
								</svg>
							</button>
						</div>
					)}
				</div>
			</aside>

			{/* ══════════════ MAIN AREA ══════════════ */}
			<div className="flex flex-1 flex-col overflow-hidden">

				{/* top bar */}
				<header className="flex h-16 flex-shrink-0 items-center justify-between border-b border-gray-200 bg-white px-6 shadow-sm">
					<div>
						<h1 className="text-lg font-bold capitalize text-gray-800">
							{NAV.find((n) => n.id === activeTab)?.label ?? 'Dashboard'}
						</h1>
						<p className="text-xs text-gray-400">SafeFlex Admin Control Panel</p>
					</div>
					<div className="flex items-center gap-3">
						<span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-700">
							ADMIN
						</span>
						<Avatar src={user?.profileImageUrl} name={user?.name ?? 'A'} size={9} />
						<button
							type="button"
							onClick={handleLogout}
							className="hidden rounded-lg border border-gray-200 px-3 py-1.5 text-sm font-medium text-gray-700 transition hover:bg-gray-50 sm:block"
						>
							Logout
						</button>
					</div>
				</header>

				{/* scrollable content */}
				<main className="flex-1 overflow-y-auto bg-gray-100 p-6">
					{children}
				</main>
			</div>
		</div>
	)
}
