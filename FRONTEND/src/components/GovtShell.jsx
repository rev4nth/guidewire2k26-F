import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function IcDashboard({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-slate-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" />
		</svg>
	)
}
function IcDisruptions({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-slate-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
		</svg>
	)
}
function IcClaims({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-slate-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
		</svg>
	)
}
function IcStats({ active }) {
	return (
		<svg className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-slate-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
		</svg>
	)
}
function IcChevron() {
	return (
		<svg className="h-4 w-4 text-slate-500" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
			<path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
		</svg>
	)
}

const NAV = [
	{ id: 'dashboard', label: 'Dashboard', Icon: IcDashboard },
	{ id: 'disruptions', label: 'Disruptions', Icon: IcDisruptions },
	{ id: 'claims', label: 'Claims Verification', Icon: IcClaims },
	{ id: 'stats', label: 'Stats', Icon: IcStats },
]

function Avatar({ src, name, size = 9 }) {
	const dim = `h-${size} w-${size}`
	const initials = (name ?? '?')[0].toUpperCase()
	if (src) return <img src={src} alt="" className={`${dim} rounded-full border border-slate-600 object-cover`} />
	return (
		<div className={`${dim} flex items-center justify-center rounded-full bg-emerald-600 text-xs font-bold text-white`}>
			{initials}
		</div>
	)
}

export default function GovtShell({ activeTab, onTabChange, children }) {
	const { user, logout } = useAuth()
	const navigate = useNavigate()
	const [sidebarOpen, setSidebarOpen] = useState(true)
	const [mobileNavOpen, setMobileNavOpen] = useState(false)

	function handleLogout() {
		logout()
		navigate('/login', { replace: true })
	}

	return (
		<div className="flex h-screen overflow-hidden bg-slate-100">
			{mobileNavOpen && (
				<button
					type="button"
					className="fixed inset-0 z-40 bg-black/50 md:hidden"
					aria-label="Close menu"
					onClick={() => setMobileNavOpen(false)}
				/>
			)}
			<aside
				className={`fixed inset-y-0 left-0 z-50 flex flex-col bg-slate-900 transition-all duration-300 ease-in-out md:static md:z-auto ${
					sidebarOpen ? 'w-64' : 'w-16'
				} ${mobileNavOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}
			>
				<div className="flex h-16 items-center justify-between border-b border-slate-800 px-4">
					{sidebarOpen && (
						<div className="flex items-center gap-2">
							<div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-600">
								<svg className="h-4 w-4 text-white" fill="currentColor" viewBox="0 0 20 20">
									<path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" />
								</svg>
							</div>
							<span className="text-base font-extrabold tracking-tight text-white">SafeFlex Gov</span>
						</div>
					)}
					<button
						type="button"
						onClick={() => setSidebarOpen((s) => !s)}
						className="rounded-md p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
						aria-label="Toggle sidebar"
					>
						<IcChevron />
					</button>
				</div>

				<nav className="flex-1 space-y-1 overflow-y-auto px-2 py-4">
					{NAV.map(({ id, label, Icon }) => {
						const active = activeTab === id
						return (
							<button
								key={id}
								type="button"
								onClick={() => {
									onTabChange(id)
									setMobileNavOpen(false)
								}}
								className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150 ${
									active
										? 'bg-emerald-800 text-white shadow-sm'
										: 'text-slate-400 hover:bg-slate-800 hover:text-white'
								}`}
							>
								<Icon active={active} />
								{sidebarOpen && <span className="truncate text-left">{label}</span>}
							</button>
						)
					})}
				</nav>

				<div className="border-t border-slate-800 p-3">
					{sidebarOpen ? (
						<div className="flex items-center gap-3">
							<Avatar src={user?.profileImageUrl} name={user?.name ?? 'G'} size={8} />
							<div className="min-w-0 flex-1">
								<p className="truncate text-sm font-semibold text-white">{user?.name ?? 'Government'}</p>
								<p className="truncate text-xs text-slate-400">{user?.email ?? ''}</p>
							</div>
							<button
								type="button"
								onClick={handleLogout}
								title="Logout"
								className="rounded-md p-1.5 text-slate-400 hover:bg-slate-800 hover:text-red-400"
							>
								<svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
								</svg>
							</button>
						</div>
					) : (
						<div className="flex flex-col items-center gap-2">
							<Avatar src={user?.profileImageUrl} name={user?.name ?? 'G'} size={8} />
							<button type="button" onClick={handleLogout} title="Logout" className="rounded-md p-1 text-slate-400 hover:text-red-400">
								<svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
								</svg>
							</button>
						</div>
					)}
				</div>
			</aside>

			<div className="flex min-w-0 flex-1 flex-col overflow-hidden">
				<header className="flex h-16 flex-shrink-0 items-center justify-between border-b border-slate-200 bg-white px-4 shadow-sm sm:px-6">
					<div className="flex min-w-0 items-center gap-3">
						<button
							type="button"
							className="rounded-md p-2 text-slate-600 hover:bg-slate-100 md:hidden"
							aria-label="Open menu"
							onClick={() => setMobileNavOpen(true)}
						>
							<svg className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
								<path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
							</svg>
						</button>
						<div className="min-w-0">
							<h1 className="truncate text-lg font-bold capitalize text-slate-800">
								{NAV.find((n) => n.id === activeTab)?.label ?? 'Dashboard'}
							</h1>
							<p className="text-xs text-slate-500">National disruption &amp; claims oversight</p>
						</div>
					</div>
					<div className="flex items-center gap-2 sm:gap-3">
						<span className="rounded-full bg-emerald-100 px-2.5 py-1 text-[10px] font-bold text-emerald-800 sm:text-xs">
							GOVT
						</span>
						<Avatar src={user?.profileImageUrl} name={user?.name ?? 'G'} size={9} />
						<button
							type="button"
							onClick={handleLogout}
							className="hidden rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 sm:block"
						>
							Logout
						</button>
					</div>
				</header>

				<main className="flex-1 overflow-y-auto bg-slate-100 p-4 sm:p-6">{children}</main>
			</div>
		</div>
	)
}
