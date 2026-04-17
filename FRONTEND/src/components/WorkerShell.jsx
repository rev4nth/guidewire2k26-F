import { useState } from 'react'
import { useAuth } from '../context/AuthContext'

const NAV = [
	{
		id: 'dashboard',
		label: 'Dashboard',
		icon: (
			<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
			</svg>
		),
	},
	{
		id: 'orders',
		label: 'Orders',
		icon: (
			<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
			</svg>
		),
	},
	{
		id: 'claims',
		label: 'Claims',
		icon: (
			<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
			</svg>
		),
	},
	{
		id: 'wallet',
		label: 'Wallet',
		icon: (
			<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M21 12a2.25 2.25 0 00-2.25-2.25H5.25A2.25 2.25 0 003 12m18 0v6a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18v-6m18 0V9M3 12V9m0 4.5h15.75a1.5 1.5 0 001.5-1.5v-3a1.5 1.5 0 00-1.5-1.5H3" />
			</svg>
		),
	},
	{
		id: 'policies',
		label: 'Policies',
		icon: (
			<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" />
			</svg>
		),
	},
	{
		id: 'profile',
		label: 'My Profile',
		icon: (
			<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
			</svg>
		),
	},
]

const TAB_TITLES = {
	dashboard: 'Dashboard',
	orders: 'My Orders',
	claims: 'My Claims',
	wallet: 'Wallet',
	policies: 'Insurance Policies',
	profile: 'My Profile',
}

export default function WorkerShell({ activeTab, onTabChange, children, pendingCount = 0 }) {
	const { user, logout } = useAuth()
	const [collapsed, setCollapsed] = useState(false)
	const [mobileNavOpen, setMobileNavOpen] = useState(false)

	const initials = (user?.name ?? 'W')[0].toUpperCase()

	return (
		<div className="flex h-screen overflow-hidden bg-gray-100">
			{mobileNavOpen && (
				<button
					type="button"
					className="fixed inset-0 z-40 bg-black/50 md:hidden"
					aria-label="Close menu"
					onClick={() => setMobileNavOpen(false)}
				/>
			)}
			{/* ── Sidebar ── */}
			<aside
				className={`fixed inset-y-0 left-0 z-50 flex flex-shrink-0 flex-col bg-gray-900 text-white transition-all duration-300 md:static md:z-auto ${
					collapsed ? 'w-16' : 'w-60'
				} ${mobileNavOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}
			>
				{/* logo row */}
				<div className={`flex h-16 items-center border-b border-gray-700 px-4 ${collapsed ? 'justify-center' : 'justify-between'}`}>
					{!collapsed && (
						<span className="text-lg font-bold tracking-tight text-white">
							Safe<span className="text-blue-400">Flex</span>
						</span>
					)}
					<button
						onClick={() => setCollapsed((c) => !c)}
						className="rounded p-1 text-gray-400 hover:bg-gray-700 hover:text-white"
					>
						<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
							<path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
						</svg>
					</button>
				</div>

				{/* nav items */}
				<nav className="flex-1 overflow-y-auto py-3">
					{NAV.map((item) => {
						const active = activeTab === item.id
						return (
							<button
								key={item.id}
								onClick={() => {
									onTabChange(item.id)
									setMobileNavOpen(false)
								}}
								className={`relative flex w-full items-center gap-3 px-4 py-2.5 text-sm font-medium transition-colors duration-150
									${active ? 'bg-gray-700 text-white' : 'text-gray-400 hover:bg-gray-800 hover:text-white'}`}
							>
								{item.icon}
								{!collapsed && <span>{item.label}</span>}
								{item.id === 'orders' && pendingCount > 0 && (
									<span className={`rounded-full bg-orange-500 px-1.5 py-0.5 text-[10px] font-bold text-white ${collapsed ? 'absolute right-1 top-1' : 'ml-auto'}`}>
										{pendingCount}
									</span>
								)}
							</button>
						)
					})}
				</nav>

				{/* user footer */}
				<div className={`border-t border-gray-700 p-3 ${collapsed ? 'flex justify-center' : 'flex items-center gap-3'}`}>
					{user?.profileImageUrl ? (
						<img src={user.profileImageUrl} alt="" className="h-8 w-8 flex-shrink-0 rounded-full border border-gray-600 object-cover" />
					) : (
						<div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-blue-500 text-xs font-bold">
							{initials}
						</div>
					)}
					{!collapsed && (
						<>
							<div className="min-w-0 flex-1">
								<p className="truncate text-xs font-semibold text-white">{user?.name ?? 'Worker'}</p>
								<p className="truncate text-[10px] text-gray-400">{user?.email}</p>
							</div>
							<button
								onClick={logout}
								title="Logout"
								className="flex-shrink-0 rounded p-1 text-gray-400 hover:text-red-400"
							>
								<svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
								</svg>
							</button>
						</>
					)}
				</div>
			</aside>

			{/* ── Main ── */}
			<div className="flex min-w-0 flex-1 flex-col overflow-hidden md:pl-0">
				{/* top bar */}
				<header className="flex h-16 flex-shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4 shadow-sm sm:px-6">
					<div className="flex min-w-0 items-center gap-3">
						<button
							type="button"
							className="rounded-md p-2 text-gray-600 hover:bg-gray-100 md:hidden"
							aria-label="Open menu"
							onClick={() => setMobileNavOpen(true)}
						>
							<svg className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
								<path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
							</svg>
						</button>
						<div className="min-w-0">
							<h1 className="truncate text-base font-bold text-gray-800">{TAB_TITLES[activeTab] ?? 'Dashboard'}</h1>
							<p className="text-xs text-gray-400">SafeFlex Worker Panel</p>
						</div>
					</div>
					<div className="flex items-center gap-3">
						<span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
							WORKER
						</span>
						{user?.profileImageUrl ? (
							<img
								src={user.profileImageUrl}
								alt=""
								className="h-8 w-8 cursor-pointer rounded-full border border-gray-200 object-cover"
								onClick={() => onTabChange('profile')}
							/>
						) : (
							<button
								onClick={() => onTabChange('profile')}
								className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-xs font-bold text-blue-600"
							>
								{initials}
							</button>
						)}
						<button
							onClick={logout}
							className="rounded-md border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-50"
						>
							Logout
						</button>
					</div>
				</header>

				{/* scroll body */}
				<main className="flex-1 overflow-y-auto p-6">{children}</main>
			</div>
		</div>
	)
}
