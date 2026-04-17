import { useCallback, useEffect, useRef, useState } from 'react'
import api from '../api/api'
import { useAuth } from '../context/AuthContext'
import AdminShell from '../components/AdminShell'

/* ─────────── helpers ─────────── */
function parseApiError(data) {
	if (data == null) return 'Something went wrong.'
	if (typeof data === 'object' && data.status === 'FAILED' && data.error) return String(data.error)
	if (typeof data === 'object' && data.error) return String(data.error)
	if (typeof data === 'string') {
		try { return JSON.parse(data).error ?? data } catch { return data }
	}
	return 'Something went wrong.'
}

const ROLE_BADGE = {
	ADMIN:  'bg-blue-100 text-blue-700',
	WORKER: 'bg-emerald-100 text-emerald-700',
	GOVT:   'bg-amber-100 text-amber-700',
}
function RoleBadge({ role }) {
	return (
		<span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${ROLE_BADGE[role] ?? 'bg-gray-100 text-gray-600'}`}>
			{role}
		</span>
	)
}

function Avatar({ src, name, size = 9 }) {
	const dim = `h-${size} w-${size}`
	const initials = (name ?? '?')[0].toUpperCase()
	if (src) return <img src={src} alt={name} className={`${dim} rounded-full border border-gray-200 object-cover`} />
	return <div className={`${dim} flex items-center justify-center rounded-full bg-blue-100 text-xs font-bold text-blue-600`}>{initials}</div>
}

/* ─────────── shared ui primitives ─────────── */
function Card({ children, className = '' }) {
	return <div className={`rounded-xl bg-white shadow-sm ring-1 ring-gray-100 ${className}`}>{children}</div>
}
function CardHeader({ title, badge, right }) {
	return (
		<div className="flex items-center justify-between border-b border-gray-100 px-6 py-4">
			<div className="flex items-center gap-2">
				<h2 className="text-sm font-semibold text-gray-800">{title}</h2>
				{badge}
			</div>
			{right}
		</div>
	)
}
function StatCard({ label, value, icon, accent }) {
	return (
		<div className={`flex items-center gap-4 rounded-xl bg-white p-5 shadow-sm ring-1 ring-gray-100 transition-all duration-200 hover:shadow-md hover:-translate-y-0.5`}>
			<div className={`flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl ${accent}`}>{icon}</div>
			<div>
				<p className="text-2xl font-bold text-gray-800">{value}</p>
				<p className="text-xs text-gray-500">{label}</p>
			</div>
		</div>
	)
}
function Field({ label, children }) {
	return (
		<div>
			<label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gray-500">{label}</label>
			{children}
		</div>
	)
}
const INPUT = 'w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-800 outline-none transition-all duration-200 focus:border-blue-400 focus:ring-2 focus:ring-blue-100 placeholder:text-gray-400'
const SELECT = 'w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-800 outline-none transition-all duration-200 focus:border-blue-400 focus:ring-2 focus:ring-blue-100'

function Badge({ count, color = 'bg-orange-100 text-orange-700' }) {
	if (!count) return null
	return <span className={`rounded-full px-2 py-0.5 text-xs font-bold ${color}`}>{count}</span>
}
function Toast({ msg }) {
	if (!msg) return null
	return (
		<div className={`flex items-start gap-2 rounded-lg p-3 text-sm font-medium ${msg.type === 'ok' ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200' : 'bg-red-50 text-red-700 ring-1 ring-red-200'}`}>
			{msg.text}
		</div>
	)
}
function EmptyState({ icon, text }) {
	return (
		<div className="flex flex-col items-center justify-center gap-3 py-16">
			<div className="flex h-14 w-14 items-center justify-center rounded-full bg-gray-50">{icon}</div>
			<p className="text-sm text-gray-400">{text}</p>
		</div>
	)
}
function RefreshBtn({ onClick }) {
	return (
		<button type="button" onClick={onClick} className="flex items-center gap-1.5 rounded-md border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-600 transition hover:bg-gray-50">
			<svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
			</svg>
			Refresh
		</button>
	)
}

/* ════════════════════ TAB PANELS ════════════════════ */

/* ── OVERVIEW (Dashboard) ── */
function TabDashboard({ users, pending, loadError, stats }) {
	const total   = stats?.totalUsers   ?? users.length
	const workers = users.filter((u) => u.role === 'WORKER').length
	const govt    = users.filter((u) => u.role === 'GOVT').length
	const pCount  = pending.length
	const totalOrders      = stats?.totalOrders      ?? 0
	const totalDisruptions = stats?.totalDisruptions ?? 0
	const totalClaims      = stats?.totalClaims      ?? 0

	return (
		<div className="space-y-6">
			{loadError ? (
				<div className="flex items-center gap-2 rounded-lg bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">
					<svg className="h-4 w-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
						<path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm-.75-11a.75.75 0 011.5 0v4a.75.75 0 01-1.5 0V7zm.75 7.25a.75.75 0 100-1.5.75.75 0 000 1.5z" clipRule="evenodd" />
					</svg>
					{loadError}
				</div>
			) : null}

			{/* top stats row */}
			<div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
				<StatCard label="Total users" value={total} accent="bg-blue-50"
					icon={<svg className="h-6 w-6 text-blue-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0" /></svg>}
				/>
				<StatCard label="Total orders" value={totalOrders} accent="bg-purple-50"
					icon={<svg className="h-6 w-6 text-purple-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>}
				/>
				<StatCard label="Disruptions" value={totalDisruptions} accent={totalDisruptions > 0 ? 'bg-red-50' : 'bg-gray-50'}
					icon={<svg className={`h-6 w-6 ${totalDisruptions > 0 ? 'text-red-400' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" /></svg>}
				/>
				<StatCard label="Claims filed" value={totalClaims} accent="bg-emerald-50"
					icon={<svg className="h-6 w-6 text-emerald-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
				/>
			</div>

			{/* second row */}
			<div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
				<StatCard label="Workers" value={workers} accent="bg-emerald-50"
					icon={<svg className="h-6 w-6 text-emerald-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M20.25 14.15v4.073a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25v-4.073M12 3v10.5m0 0l-3-3m3 3l3-3" /></svg>}
				/>
				<StatCard label="Govt users" value={govt} accent="bg-amber-50"
					icon={<svg className="h-6 w-6 text-amber-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.332A48.36 48.36 0 0012 9.75c-2.551 0-5.056.2-7.5.582V21" /></svg>}
				/>
				<StatCard label="Pending" value={pCount} accent={pCount > 0 ? 'bg-orange-50' : 'bg-gray-50'}
					icon={<svg className={`h-6 w-6 ${pCount > 0 ? 'text-orange-400' : 'text-gray-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
				/>
				<StatCard label="Policies" value={3} accent="bg-violet-50"
					icon={<svg className="h-6 w-6 text-violet-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" /></svg>}
				/>
			</div>

			{/* quick tip cards */}
			<div className="grid gap-4 sm:grid-cols-3">
				{[
					{ label: 'Manage users',        sub: 'View, create, and delete user accounts.',     color: 'bg-blue-500' },
					{ label: 'Review registrations',sub: 'Approve or reject pending sign-up requests.', color: 'bg-orange-500' },
					{ label: 'Policy pricing',      sub: 'Edit premium & coverage for each plan.',      color: 'bg-violet-500' },
				].map((c) => (
					<div key={c.label} className="flex flex-col gap-2 rounded-xl bg-white p-5 shadow-sm ring-1 ring-gray-100 transition hover:shadow-md">
						<div className={`h-1.5 w-10 rounded-full ${c.color}`} />
						<p className="font-semibold text-gray-800">{c.label}</p>
						<p className="text-xs text-gray-400">{c.sub}</p>
					</div>
				))}
			</div>
		</div>
	)
}

/* ── USERS tab ── */
function TabUsers({ users, pending, loadError, busyId, approve, reject, removeUser, handleCreateUser, loadAll,
	newName, setNewName, newEmail, setNewEmail, newPassword, setNewPassword,
	newRole, setNewRole, newLocation, setNewLocation,
	newPhotoPreview, newPhotoFile, setNewPhotoFile, newPhotoUploading,
	creating, formMsg }) {

	const total = users.length
	const pCount = pending.length

	return (
		<div className="space-y-6">
			{loadError ? (
				<div className="flex items-center gap-2 rounded-lg bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">
					{loadError}
				</div>
			) : null}

			{/* Pending registrations */}
			<Card>
				<CardHeader
					title="Pending registrations"
					badge={<Badge count={pCount} />}
					right={<span className="text-xs text-gray-400">Auto-refreshes every 12 s</span>}
				/>
				{pCount === 0 ? (
					<EmptyState
						text="All clear — no pending registrations"
						icon={<svg className="h-7 w-7 text-gray-300" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
					/>
				) : (
					<div className="overflow-x-auto">
						<table className="min-w-full text-sm">
							<thead>
								<tr className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
									<th className="w-14 px-5 py-3">Photo</th>
									<th className="px-5 py-3">Name</th>
									<th className="px-5 py-3">Email</th>
									<th className="px-5 py-3">Role</th>
									<th className="px-5 py-3">Location</th>
									<th className="px-5 py-3">Requested</th>
									<th className="px-5 py-3 text-right">Actions</th>
								</tr>
							</thead>
							<tbody className="divide-y divide-gray-50">
								{pending.map((row) => (
									<tr key={row.id} className="transition-colors duration-150 hover:bg-blue-50/30">
										<td className="px-5 py-3"><Avatar src={row.profileImageUrl} name={row.name} /></td>
										<td className="px-5 py-3 font-medium text-gray-900">{row.name}</td>
										<td className="px-5 py-3 text-gray-500">{row.email}</td>
										<td className="px-5 py-3"><RoleBadge role={row.role} /></td>
										<td className="px-5 py-3 text-gray-400">{row.location ?? '—'}</td>
										<td className="px-5 py-3 text-xs text-gray-400">{row.createdAt ? new Date(row.createdAt).toLocaleString() : '—'}</td>
										<td className="px-5 py-3 text-right">
											<div className="flex justify-end gap-2">
												<button type="button" disabled={busyId === row.id} onClick={() => approve(row.id)}
													className="rounded-md bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-600 disabled:opacity-40">
													{busyId === row.id ? '…' : 'Approve'}
												</button>
												<button type="button" disabled={busyId === row.id} onClick={() => reject(row.id)}
													className="rounded-md border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-700 transition hover:bg-gray-100 disabled:opacity-40">
													Reject
												</button>
											</div>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				)}
			</Card>

			{/* Create user */}
			<Card className="p-6">
				<div className="mb-5 flex items-center gap-3">
					<div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-50">
						<svg className="h-5 w-5 text-blue-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
							<path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
						</svg>
					</div>
					<div>
						<h2 className="text-sm font-semibold text-gray-800">Create user</h2>
						<p className="text-xs text-gray-400">Immediately adds account — no approval step</p>
					</div>
				</div>
				{formMsg ? <div className="mb-4"><Toast msg={formMsg} /></div> : null}
				<form onSubmit={handleCreateUser} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
					<Field label="Full name"><input required value={newName} onChange={(e) => setNewName(e.target.value)} className={INPUT} placeholder="Jane Doe" /></Field>
					<Field label="Email"><input type="email" required value={newEmail} onChange={(e) => setNewEmail(e.target.value)} className={INPUT} placeholder="jane@example.com" /></Field>
					<Field label="Password"><input type="password" required value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className={INPUT} placeholder="••••••••" /></Field>
					<Field label="Role">
						<select value={newRole} onChange={(e) => setNewRole(e.target.value)} className={SELECT}>
							<option value="WORKER">WORKER</option>
							<option value="GOVT">GOVT</option>
							<option value="ADMIN">ADMIN</option>
						</select>
					</Field>
					<Field label="Location"><input value={newLocation} onChange={(e) => setNewLocation(e.target.value)} className={INPUT} placeholder="City, Country" /></Field>
					<Field label="Profile photo (optional)">
						<div className="flex items-center gap-3">
							{/* circular preview */}
							<div className="relative flex-shrink-0">
								{newPhotoPreview ? (
									<img src={newPhotoPreview} alt="preview"
										className="h-11 w-11 rounded-full border-2 border-blue-200 object-cover shadow" />
								) : (
									<div className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-dashed border-gray-300 bg-gray-50 text-gray-400">
										<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
											<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 10.5l4.72-4.72a.75.75 0 011.28.53v11.38a.75.75 0 01-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 002.25-2.25v-9a2.25 2.25 0 00-2.25-2.25h-9A2.25 2.25 0 002.25 7.5v9a2.25 2.25 0 002.25 2.25z" />
										</svg>
									</div>
								)}
								{newPhotoUploading && (
									<div className="absolute inset-0 flex items-center justify-center rounded-full bg-white/70">
										<svg className="h-4 w-4 animate-spin text-blue-500" viewBox="0 0 24 24" fill="none">
											<circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
											<path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
										</svg>
									</div>
								)}
							</div>
							{/* picker */}
							<label className="flex cursor-pointer items-center gap-2 rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 transition hover:bg-gray-50">
								<svg className="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
								</svg>
								{newPhotoFile ? newPhotoFile.name : 'Add photo'}
								<input
									type="file"
									accept="image/*"
									className="hidden"
									onChange={(e) => setNewPhotoFile(e.target.files?.[0] ?? null)}
								/>
							</label>
							{newPhotoFile && (
								<button type="button" onClick={() => setNewPhotoFile(null)}
									className="rounded-full p-1 text-gray-400 hover:text-red-500">
									<svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
										<path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
									</svg>
								</button>
							)}
						</div>
					</Field>
					<div className="flex items-end sm:col-span-2 lg:col-span-3">
						<button type="submit" disabled={creating}
							className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:opacity-50">
							{creating ? (
								<><svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" /></svg>Creating…</>
							) : (
								<><svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={2.2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>Create user</>
							)}
						</button>
					</div>
				</form>
			</Card>

			{/* All users */}
			<Card>
				<CardHeader
					title={<>All users <span className="ml-1 rounded-full bg-gray-100 px-2 py-0.5 text-xs font-semibold text-gray-500">{total}</span></>}
					right={<RefreshBtn onClick={loadAll} />}
				/>
				{total === 0 ? (
					<EmptyState text="No users found"
						icon={<svg className="h-7 w-7 text-gray-300" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" /></svg>}
					/>
				) : (
					<div className="overflow-x-auto">
						<table className="min-w-full text-sm">
							<thead>
								<tr className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
									<th className="px-5 py-3 text-center">ID</th>
									<th className="px-5 py-3 text-center">Photo</th>
									<th className="px-5 py-3">Name</th>
									<th className="px-5 py-3">Email</th>
									<th className="px-5 py-3">Role</th>
									<th className="px-5 py-3">Location</th>
									<th className="px-5 py-3 text-right">Actions</th>
								</tr>
							</thead>
							<tbody className="divide-y divide-gray-50">
								{users.map((u) => (
									<tr key={u.id} className="transition-colors duration-150 hover:bg-gray-50/70">
										<td className="px-5 py-3 text-center font-mono text-xs text-gray-400">{u.id}</td>
										<td className="px-5 py-3 text-center"><div className="flex justify-center"><Avatar src={u.profileImageUrl} name={u.name} /></div></td>
										<td className="px-5 py-3 font-semibold text-gray-800">{u.name}</td>
										<td className="px-5 py-3 text-gray-500">{u.email}</td>
										<td className="px-5 py-3"><RoleBadge role={u.role} /></td>
										<td className="px-5 py-3 text-gray-500">{u.location ?? '—'}</td>
										<td className="px-5 py-3 text-right">
											<button type="button" disabled={busyId === `u-${u.id}`} onClick={() => removeUser(u.id)}
												className="rounded-md border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:bg-red-100 disabled:opacity-40">
												{busyId === `u-${u.id}` ? '…' : 'Delete'}
											</button>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				)}
			</Card>
		</div>
	)
}

/* ── POLICIES tab ── */
function TabPolicies({ policies, onSavePolicy }) {
	const [edits, setEdits] = useState({})
	const [saving, setSaving] = useState(null)
	const [msg, setMsg] = useState(null)

	function setField(id, field, val) {
		setEdits((prev) => ({ ...prev, [id]: { ...(prev[id] ?? {}), [field]: val } }))
	}

	async function save(policy) {
		setSaving(policy.id)
		setMsg(null)
		try {
			await onSavePolicy(policy.id, {
				premium: parseFloat(edits[policy.id]?.premium ?? policy.premium),
				coverage: parseFloat(edits[policy.id]?.coverage ?? policy.coverage),
			})
			setMsg({ id: policy.id, type: 'ok', text: 'Saved!' })
		} catch {
			setMsg({ id: policy.id, type: 'err', text: 'Save failed.' })
		} finally {
			setSaving(null)
		}
	}

	const PLAN_COLOR = { BASIC: 'text-blue-700 bg-blue-50', PRO: 'text-purple-700 bg-purple-50', PREMIUM: 'text-amber-700 bg-amber-50' }

	return (
		<div className="space-y-4">
			<p className="text-sm text-gray-500">Edit premium and coverage for each plan. Changes apply immediately.</p>
			<Card>
				<div className="overflow-x-auto">
					<table className="min-w-full text-sm">
						<thead>
							<tr className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
								<th className="px-6 py-4">Plan</th>
								<th className="px-6 py-4">Premium (₹/mo)</th>
								<th className="px-6 py-4">Coverage (₹)</th>
								<th className="px-6 py-4">Status</th>
								<th className="px-6 py-4 text-right">Action</th>
							</tr>
						</thead>
						<tbody className="divide-y divide-gray-50">
							{policies.map((p) => {
								const color = PLAN_COLOR[p.name.toUpperCase()] ?? 'text-gray-700 bg-gray-50'
								const rowMsg = msg?.id === p.id ? msg : null
								return (
									<tr key={p.id} className="hover:bg-gray-50/60">
										<td className="px-6 py-4">
											<span className={`rounded-full px-3 py-1 text-xs font-bold ${color}`}>{p.name}</span>
										</td>
										<td className="px-6 py-4">
											<input
												type="number"
												min="0"
												step="0.01"
												defaultValue={p.premium}
												onChange={(e) => setField(p.id, 'premium', e.target.value)}
												className="w-28 rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
											/>
										</td>
										<td className="px-6 py-4">
											<input
												type="number"
												min="0"
												step="0.01"
												defaultValue={p.coverage}
												onChange={(e) => setField(p.id, 'coverage', e.target.value)}
												className="w-32 rounded-md border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
											/>
										</td>
										<td className="px-6 py-4">
											<span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${p.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
												{p.active ? 'Active' : 'Inactive'}
											</span>
										</td>
										<td className="px-6 py-4 text-right">
											<div className="flex items-center justify-end gap-3">
												{rowMsg && (
													<span className={`text-xs font-medium ${rowMsg.type === 'ok' ? 'text-emerald-600' : 'text-red-600'}`}>
														{rowMsg.text}
													</span>
												)}
												<button
													type="button"
													disabled={saving === p.id}
													onClick={() => save(p)}
													className="rounded-md bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white transition hover:bg-blue-700 disabled:opacity-50"
												>
													{saving === p.id ? 'Saving…' : 'Save'}
												</button>
											</div>
										</td>
									</tr>
								)
							})}
						</tbody>
					</table>
					{policies.length === 0 && (
						<p className="py-12 text-center text-sm text-gray-400">No policies found.</p>
					)}
				</div>
			</Card>
		</div>
	)
}

/* ── DISRUPTIONS tab ── */
function TabDisruptions({ users, disruptions, loadDisruptions, onSendOrder, onTriggerDisruption, onBroadcastDisruption, onSendToNearest }) {
	const workers = users.filter((u) => u.role === 'WORKER')
	const [selectedWorker, setSelectedWorker] = useState(null)
	const [workerDetail, setWorkerDetail]     = useState(null)
	const [detailLoading, setDetailLoading]   = useState(false)
	const [disruptType, setDisruptType]       = useState('RAIN')
	const [disruptSev, setDisruptSev]         = useState('HIGH')
	const [actionMsg, setActionMsg]           = useState(null)
	const [busyAction, setBusyAction]         = useState(false)

	// Broadcast controls
	const [broadcastCity, setBroadcastCity]     = useState('')
	const [broadcastType, setBroadcastType]     = useState('RAIN')
	const [broadcastSev, setBroadcastSev]       = useState('HIGH')
	const [broadcastMsg, setBroadcastMsg]       = useState(null)
	const [broadcastBusy, setBroadcastBusy]     = useState(false)

	// Send-to-nearest controls
	const [nearestLat, setNearestLat]           = useState('')
	const [nearestLon, setNearestLon]           = useState('')
	const [nearestRadius, setNearestRadius]     = useState('50')
	const [nearestMsg, setNearestMsg]           = useState(null)
	const [nearestBusy, setNearestBusy]         = useState(false)

	async function handleBroadcast(e) {
		e.preventDefault()
		if (!broadcastCity.trim()) return
		setBroadcastBusy(true); setBroadcastMsg(null)
		try {
			const res = await onBroadcastDisruption(broadcastCity.trim(), broadcastType, broadcastSev)
			setBroadcastMsg({ type: 'ok', text: `Broadcast to ${res.affectedCount} worker(s) in "${broadcastCity}".` })
			await loadDisruptions()
		} catch (err) {
			setBroadcastMsg({ type: 'err', text: err.response?.data?.error ?? 'Broadcast failed.' })
		} finally { setBroadcastBusy(false) }
	}

	async function handleSendNearest(e) {
		e.preventDefault()
		const lat = parseFloat(nearestLat)
		const lon = parseFloat(nearestLon)
		if (isNaN(lat) || isNaN(lon)) { setNearestMsg({ type: 'err', text: 'Enter valid lat/lon.' }); return }
		setNearestBusy(true); setNearestMsg(null)
		try {
			const res = await onSendToNearest(lat, lon, parseFloat(nearestRadius) || 50)
			setNearestMsg({ type: 'ok', text: `Order sent to ${res.workerName} (${res.distanceKm} km away).` })
		} catch (err) {
			setNearestMsg({ type: 'err', text: err.response?.data?.error ?? 'No worker found nearby.' })
		} finally { setNearestBusy(false) }
	}

	async function openWorker(worker) {
		setSelectedWorker(worker)
		setDetailLoading(true)
		setActionMsg(null)
		try {
			const res = await api.get(`/admin/workers/${worker.id}`)
			setWorkerDetail(res.data)
		} catch { setWorkerDetail(null) }
		finally { setDetailLoading(false) }
	}

	async function sendOrder() {
		if (!selectedWorker) return
		setBusyAction(true); setActionMsg(null)
		try {
			await onSendOrder(selectedWorker.id)
			setActionMsg({ type: 'ok', text: 'Order sent!' })
			await openWorker(selectedWorker)
		} catch (e) { setActionMsg({ type: 'err', text: e.response?.data?.error ?? 'Failed' }) }
		finally { setBusyAction(false) }
	}

	async function triggerDisruption() {
		if (!selectedWorker) return
		setBusyAction(true); setActionMsg(null)
		try {
			const res = await onTriggerDisruption(selectedWorker.id, disruptType, disruptSev)
			const msg = res.claimCreated
				? `Disruption triggered. Order cancelled. ₹${Number(res.claimAmount).toLocaleString()} claim created.`
				: `Disruption triggered. No active order.`
			setActionMsg({ type: 'ok', text: msg })
			await loadDisruptions()
			await openWorker(selectedWorker)
		} catch (e) { setActionMsg({ type: 'err', text: e.response?.data?.error ?? 'Failed' }) }
		finally { setBusyAction(false) }
	}

	const SEV_BADGE = {
		HIGH: 'bg-red-100 text-red-700',
		MEDIUM: 'bg-amber-100 text-amber-800',
		LOW: 'bg-yellow-100 text-yellow-700',
	}
	const TYPE_BADGE = {
		RAIN: 'bg-blue-100 text-blue-700',
		TRAFFIC: 'bg-orange-100 text-orange-700',
		BANDH: 'bg-purple-100 text-purple-700',
		FLOOD: 'bg-cyan-100 text-cyan-800',
	}
	const SOURCE_BADGE = { AUTO: 'bg-blue-100 text-blue-800', MANUAL: 'bg-amber-100 text-amber-900', GOVT: 'bg-emerald-100 text-emerald-900' }
	const CLAIM_STATUS_MINI = { APPROVED: 'bg-emerald-200 text-emerald-900', REVIEW: 'bg-amber-200 text-amber-950', REJECTED: 'bg-red-200 text-red-900' }
	const ORDER_STATUS_BADGE = {
		PENDING: 'bg-orange-100 text-orange-700', ACCEPTED: 'bg-blue-100 text-blue-700',
		PICKED_UP: 'bg-purple-100 text-purple-700', DELIVERED: 'bg-emerald-100 text-emerald-700',
		CANCELLED: 'bg-red-100 text-red-600',
	}

	return (
		<div className="space-y-6">
			<div className="grid gap-6 lg:grid-cols-5">
				{/* Worker list */}
				<Card className="lg:col-span-2">
					<CardHeader title="Workers" right={<span className="text-xs text-gray-400">Click to view detail</span>} />
					{workers.length === 0 ? (
						<p className="py-8 text-center text-sm text-gray-400">No workers found</p>
					) : (
						<div className="divide-y divide-gray-50">
							{workers.map((w) => (
								<button
									key={w.id}
									type="button"
									onClick={() => openWorker(w)}
									className={`flex w-full items-center gap-3 px-5 py-3 text-left transition hover:bg-blue-50/40 ${selectedWorker?.id === w.id ? 'bg-blue-50 ring-l-2 ring-blue-400' : ''}`}
								>
									<Avatar src={w.profileImageUrl} name={w.name} />
									<div className="min-w-0 flex-1">
										<p className="truncate text-sm font-semibold text-gray-800">{w.name}</p>
										<div className="flex items-center gap-1.5">
											{w.location && (
												<span className="flex items-center gap-0.5 text-xs text-gray-400">
													<svg className="h-3 w-3" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
														<path strokeLinecap="round" strokeLinejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" />
														<path strokeLinecap="round" strokeLinejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" />
													</svg>
													{w.location}
												</span>
											)}
											{w.latitude != null && (
												<span className="rounded-full bg-emerald-100 px-1.5 py-0.5 text-[9px] font-bold text-emerald-700">GPS</span>
											)}
										</div>
									</div>
									<svg className="h-4 w-4 text-gray-300" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
										<path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
									</svg>
								</button>
							))}
						</div>
					)}
				</Card>

				{/* Worker detail */}
				<Card className="lg:col-span-3">
					{!selectedWorker ? (
						<div className="flex flex-col items-center justify-center py-20 text-gray-400">
							<svg className="mb-3 h-10 w-10" fill="none" stroke="currentColor" strokeWidth={1.2} viewBox="0 0 24 24">
								<path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0" />
							</svg>
							<p className="text-sm">Select a worker to view details</p>
						</div>
					) : detailLoading ? (
						<div className="flex items-center justify-center py-20 text-gray-400 text-sm">Loading…</div>
					) : (
						<div>
							<CardHeader
								title={selectedWorker.name}
								badge={<RoleBadge role="WORKER" />}
								right={
									<button onClick={() => { setSelectedWorker(null); setWorkerDetail(null) }}
										className="text-gray-400 hover:text-gray-600">
										<svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
											<path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
										</svg>
									</button>
								}
							/>
							<div className="space-y-4 p-5">
								{/* Actions */}
								<div className="flex flex-wrap items-end gap-3 rounded-xl bg-gray-50 p-4">
									<button
										type="button"
										disabled={busyAction}
										onClick={sendOrder}
										className="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:opacity-50"
									>
										{busyAction ? '…' : 'Send Order'}
									</button>
									<div className="flex items-center gap-2">
										<select value={disruptType} onChange={(e) => setDisruptType(e.target.value)}
											className="rounded-md border border-gray-300 px-2 py-2 text-sm">
											<option>RAIN</option>
											<option>TRAFFIC</option>
											<option>BANDH</option>
											<option>FLOOD</option>
										</select>
										<select value={disruptSev} onChange={(e) => setDisruptSev(e.target.value)}
											className="rounded-md border border-gray-300 px-2 py-2 text-sm">
											<option>HIGH</option>
											<option>MEDIUM</option>
											<option>LOW</option>
										</select>
										<button
											type="button"
											disabled={busyAction}
											onClick={triggerDisruption}
											className="rounded-md bg-red-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-600 disabled:opacity-50"
										>
											{busyAction ? '…' : 'Trigger Disruption'}
										</button>
									</div>
									{actionMsg && (
										<span className={`text-xs font-medium ${actionMsg.type === 'ok' ? 'text-emerald-600' : 'text-red-600'}`}>
											{actionMsg.text}
										</span>
									)}
								</div>

								{/* Orders */}
								<div>
									<p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-400">Orders ({workerDetail?.orders?.length ?? 0})</p>
									<div className="space-y-1 max-h-40 overflow-y-auto">
										{(workerDetail?.orders ?? []).slice(0, 10).map((o) => (
											<div key={o.id} className="flex items-center justify-between rounded-md bg-gray-50 px-3 py-1.5">
												<span className="font-mono text-xs text-gray-500">#{o.id}</span>
												<span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${ORDER_STATUS_BADGE[o.status] ?? 'bg-gray-100 text-gray-600'}`}>{o.status}</span>
											</div>
										))}
										{!workerDetail?.orders?.length && <p className="text-xs text-gray-400">No orders</p>}
									</div>
								</div>

								{/* Claims */}
								<div>
									<p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-400">Claims ({workerDetail?.claims?.length ?? 0})</p>
									<div className="space-y-1 max-h-40 overflow-y-auto">
										{(workerDetail?.claims ?? []).slice(0, 10).map((c) => (
											<div key={c.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md bg-emerald-50 px-3 py-1.5">
												<span className="text-xs text-gray-600 min-w-0 flex-1 truncate">{c.reason}</span>
												<div className="flex items-center gap-2 shrink-0">
													{c.status && (
														<span className={`rounded-full px-1.5 py-0.5 text-[9px] font-bold ${CLAIM_STATUS_MINI[c.status] ?? 'bg-gray-200 text-gray-700'}`}>{c.status}</span>
													)}
													<span className="text-[10px] text-gray-500">{c.confidenceScore ?? 0} pts</span>
													<span className="text-xs font-semibold text-emerald-700">₹{Number(c.amount).toLocaleString()}</span>
												</div>
											</div>
										))}
										{!workerDetail?.claims?.length && <p className="text-xs text-gray-400">No claims</p>}
									</div>
								</div>

								{/* Disruptions for this worker */}
								<div>
									<p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-400">Disruptions ({workerDetail?.disruptions?.length ?? 0})</p>
									<div className="space-y-1 max-h-36 overflow-y-auto">
										{(workerDetail?.disruptions ?? []).slice(0, 10).map((d) => (
											<div key={d.id} className="flex flex-wrap items-center gap-2 rounded-md bg-orange-50 px-3 py-1.5">
												<span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${TYPE_BADGE[d.type] ?? ''}`}>{d.type}</span>
												<span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${SEV_BADGE[d.severity] ?? ''}`}>{d.severity}</span>
												{d.source && (
													<span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${SOURCE_BADGE[d.source] ?? 'bg-gray-100 text-gray-600'}`}>{d.source}</span>
												)}
												<span className="ml-auto text-[10px] text-gray-400">{new Date(d.createdAt).toLocaleString()}</span>
											</div>
										))}
										{!workerDetail?.disruptions?.length && <p className="text-xs text-gray-400">No disruptions</p>}
									</div>
								</div>
							</div>
						</div>
					)}
				</Card>
			</div>

			{/* ── Location-based controls ── */}
			<div className="grid gap-6 md:grid-cols-2">

				{/* Broadcast disruption to city */}
				<Card className="p-6">
					<div className="mb-4 flex items-center gap-2">
						<div className="flex h-9 w-9 items-center justify-center rounded-lg bg-red-50">
							<svg className="h-5 w-5 text-red-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
								<path strokeLinecap="round" strokeLinejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" />
								<path strokeLinecap="round" strokeLinejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" />
							</svg>
						</div>
						<div>
							<h2 className="text-sm font-semibold text-gray-800">Trigger disruption (manual)</h2>
							<p className="text-xs text-gray-400">POST /admin/disruption — all workers in that city</p>
						</div>
					</div>
					<form onSubmit={handleBroadcast} className="space-y-3">
						<input
							required
							value={broadcastCity}
							onChange={(e) => setBroadcastCity(e.target.value)}
							placeholder="City name (e.g. Hyderabad)"
							className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-red-400 focus:ring-2 focus:ring-red-100"
						/>
						<div className="flex gap-2">
							<select value={broadcastType} onChange={(e) => setBroadcastType(e.target.value)}
								className="flex-1 rounded-md border border-gray-300 px-2 py-2 text-sm">
								<option>RAIN</option>
								<option>TRAFFIC</option>
								<option>BANDH</option>
								<option>FLOOD</option>
							</select>
							<select value={broadcastSev} onChange={(e) => setBroadcastSev(e.target.value)}
								className="flex-1 rounded-md border border-gray-300 px-2 py-2 text-sm">
								<option>HIGH</option>
								<option>MEDIUM</option>
								<option>LOW</option>
							</select>
						</div>
						{broadcastMsg && (
							<p className={`text-xs font-medium ${broadcastMsg.type === 'ok' ? 'text-emerald-600' : 'text-red-600'}`}>
								{broadcastMsg.text}
							</p>
						)}
						<button type="submit" disabled={broadcastBusy}
							className="w-full rounded-md bg-red-500 py-2 text-sm font-semibold text-white transition hover:bg-red-600 disabled:opacity-50">
							{broadcastBusy ? 'Triggering…' : 'Trigger disruption'}
						</button>
					</form>
				</Card>

				{/* Send order to nearest GPS worker */}
				<Card className="p-6">
					<div className="mb-4 flex items-center gap-2">
						<div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-50">
							<svg className="h-5 w-5 text-blue-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
								<path strokeLinecap="round" strokeLinejoin="round" d="M9 6.75V15m6-6v8.25m.503 3.498l4.875-2.437c.381-.19.622-.58.622-1.006V4.82c0-.836-.88-1.38-1.628-1.006l-3.869 1.934c-.317.159-.69.159-1.006 0L9.503 3.252a1.125 1.125 0 00-1.006 0L3.622 5.689C3.24 5.88 3 6.27 3 6.695V19.18c0 .836.88 1.38 1.628 1.006l3.869-1.934c.317-.159.69-.159 1.006 0l4.994 2.497c.317.158.69.158 1.006 0z" />
							</svg>
						</div>
						<div>
							<h2 className="text-sm font-semibold text-gray-800">Send Order to Nearest Worker</h2>
							<p className="text-xs text-gray-400">Uses GPS coordinates to find closest worker</p>
						</div>
					</div>
					<form onSubmit={handleSendNearest} className="space-y-3">
						<div className="flex gap-2">
							<input
								required
								type="number"
								step="any"
								value={nearestLat}
								onChange={(e) => setNearestLat(e.target.value)}
								placeholder="Latitude (e.g. 17.385)"
								className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
							/>
							<input
								required
								type="number"
								step="any"
								value={nearestLon}
								onChange={(e) => setNearestLon(e.target.value)}
								placeholder="Longitude (e.g. 78.486)"
								className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
							/>
						</div>
						<div className="flex items-center gap-2">
							<label className="text-xs font-semibold text-gray-500 whitespace-nowrap">Radius (km):</label>
							<input
								type="number"
								min="1"
								max="500"
								value={nearestRadius}
								onChange={(e) => setNearestRadius(e.target.value)}
								className="w-20 rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
							/>
						</div>
						{nearestMsg && (
							<p className={`text-xs font-medium ${nearestMsg.type === 'ok' ? 'text-emerald-600' : 'text-red-600'}`}>
								{nearestMsg.text}
							</p>
						)}
						<button type="submit" disabled={nearestBusy}
							className="w-full rounded-md bg-blue-600 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:opacity-50">
							{nearestBusy ? 'Finding…' : 'Send to Nearest Worker'}
						</button>
					</form>
				</Card>
			</div>

		{/* All disruptions table */}
			<Card>
				<CardHeader title="All Disruptions" right={<RefreshBtn onClick={loadDisruptions} />} />
				{disruptions.length === 0 ? (
					<p className="py-8 text-center text-sm text-gray-400">No disruptions recorded yet</p>
				) : (
					<div className="overflow-x-auto">
						<table className="min-w-full text-sm">
							<thead>
								<tr className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
									<th className="px-5 py-3">ID</th>
									<th className="px-5 py-3">Worker</th>
									<th className="px-5 py-3">Location</th>
									<th className="px-5 py-3">Type</th>
									<th className="px-5 py-3">Severity</th>
									<th className="px-5 py-3">Source</th>
									<th className="px-5 py-3">Date</th>
								</tr>
							</thead>
							<tbody className="divide-y divide-gray-50">
								{disruptions.map((d) => (
									<tr key={d.id} className="hover:bg-gray-50/60">
										<td className="px-5 py-3 font-mono text-xs text-gray-400">#{d.id}</td>
										<td className="px-5 py-3 font-semibold text-gray-800">{d.workerName}</td>
										<td className="px-5 py-3 text-xs text-gray-600">{d.location || '—'}</td>
										<td className="px-5 py-3">
											<span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${TYPE_BADGE[d.type] ?? 'bg-gray-100 text-gray-600'}`}>{d.type}</span>
										</td>
										<td className="px-5 py-3">
											<span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${SEV_BADGE[d.severity] ?? ''}`}>{d.severity}</span>
										</td>
										<td className="px-5 py-3">
											{d.source ? (
												<span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${SOURCE_BADGE[d.source] ?? 'bg-gray-100 text-gray-600'}`}>{d.source}</span>
											) : (
												<span className="text-xs text-gray-400">—</span>
											)}
										</td>
										<td className="px-5 py-3 text-xs text-gray-400">{new Date(d.createdAt).toLocaleString()}</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				)}
			</Card>
		</div>
	)
}

/* ── FINANCE tab ── */
function TabFinance({ finance, loadError }) {
	if (loadError) {
		return (
			<div className="rounded-lg bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">{loadError}</div>
		)
	}
	if (!finance) {
		return <p className="text-sm text-gray-500">Loading finance…</p>
	}
	const rev = Number(finance.totalRevenue ?? 0)
	const paid = Number(finance.totalClaimsPaid ?? 0)
	const profit = Number(finance.profit ?? 0)
	return (
		<div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
			<StatCard
				label="Total revenue"
				value={`₹${rev.toLocaleString()}`}
				accent="bg-emerald-50"
				icon={<svg className="h-6 w-6 text-emerald-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182l-.7-.525m5.314-3.182C19.33 8.25 17.7 6.75 15.75 6.75c-1.834 0-3.286.946-4.163 2.408" /></svg>}
			/>
			<StatCard
				label="Total claims paid"
				value={`₹${paid.toLocaleString()}`}
				accent="bg-orange-50"
				icon={<svg className="h-6 w-6 text-orange-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" /></svg>}
			/>
			<StatCard
				label="Net profit"
				value={`₹${profit.toLocaleString()}`}
				accent={profit >= 0 ? 'bg-blue-50' : 'bg-red-50'}
				icon={<svg className={`h-6 w-6 ${profit >= 0 ? 'text-blue-500' : 'text-red-500'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M3.75 3v11.25A2.25 2.25 0 006 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0118 16.5h-2.25m-7.5 0h7.5m-7.5 0l-1 3m8.5-3l1 3m0 0l.5 1.5m-.5-1.5h-9.5m0 0l-.5 1.5M9 11.25v1.5M12 9v3.75m3-6v6" /></svg>}
			/>
		</div>
	)
}

/* ── PROFILE tab ── */
function TabProfile({ user, photoPreview, fileInputRef, handlePhotoSelect, handlePhotoUpload, photoLoading, photoStatus }) {
	return (
		<div className="space-y-6">
			<Card className="p-6">
				<h2 className="mb-5 text-sm font-semibold text-gray-800">Account details</h2>
				<div className="flex flex-wrap items-center gap-6">
					<div className="relative">
						{(photoPreview ?? user?.profileImageUrl) ? (
							<img src={photoPreview ?? user?.profileImageUrl} alt="" className="h-20 w-20 rounded-full border-4 border-white object-cover shadow-md" />
						) : (
							<div className="flex h-20 w-20 items-center justify-center rounded-full bg-blue-100 text-2xl font-bold text-blue-600 shadow-md">
								{(user?.name ?? 'A')[0].toUpperCase()}
							</div>
						)}
						{(photoPreview ?? user?.profileImageUrl) ? (
							<span className="absolute -bottom-1 -right-1 rounded-full bg-blue-500 p-1 text-white shadow">
								<svg className="h-3 w-3" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-9" />
								</svg>
							</span>
						) : null}
					</div>
					<div className="space-y-1">
						<p className="text-lg font-bold text-gray-800">{user?.name ?? '—'}</p>
						<p className="text-sm text-gray-500">{user?.email ?? '—'}</p>
						<span className="inline-block rounded-full bg-blue-100 px-3 py-0.5 text-xs font-semibold text-blue-700">
							{user?.role ?? 'ADMIN'}
						</span>
					</div>
				</div>
			</Card>

			<Card className="p-6">
				<h2 className="mb-4 text-sm font-semibold text-gray-800">Update profile photo</h2>
				<form onSubmit={handlePhotoUpload} className="flex flex-wrap items-end gap-4">
					<div className="flex-1 min-w-0">
						<label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gray-500">
							Choose image (Cloudinary)
						</label>
						<input
							ref={fileInputRef}
							type="file"
							accept="image/*"
							onChange={handlePhotoSelect}
							className="w-full text-sm text-gray-600 file:mr-3 file:cursor-pointer file:rounded-md file:border-0 file:bg-blue-50 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-blue-700 hover:file:bg-blue-100"
						/>
					</div>
					<button
						type="submit"
						disabled={photoLoading}
						className="rounded-md bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:opacity-50"
					>
						{photoLoading ? 'Uploading…' : 'Upload photo'}
					</button>
				</form>
				{photoStatus ? (
					<p className={`mt-3 text-sm font-medium ${photoStatus.type === 'ok' ? 'text-emerald-600' : 'text-red-600'}`}>
						{photoStatus.text}
					</p>
				) : null}
			</Card>
		</div>
	)
}

/* ════════════════════════════════════════════════
   ROOT COMPONENT
════════════════════════════════════════════════ */
export default function AdminDashboard() {
	const { user, uploadProfileImage } = useAuth()
	const [activeTab, setActiveTab] = useState('dashboard')

	/* data */
	const [pending, setPending]         = useState([])
	const [users, setUsers]             = useState([])
	const [policies, setPolicies]       = useState([])
	const [disruptions, setDisruptions] = useState([])
	const [stats, setStats]             = useState(null)
	const [finance, setFinance]       = useState(null)
	const [loadError, setLoadError]     = useState('')
	const [busyId, setBusyId]           = useState(null)

	/* create-user form */
	const [newName, setNewName]       = useState('')
	const [newEmail, setNewEmail]     = useState('')
	const [newPassword, setNewPassword] = useState('')
	const [newRole, setNewRole]       = useState('WORKER')
	const [newLocation, setNewLocation] = useState('')
	const [newPhotoFile, setNewPhotoFile]         = useState(null)
	const [newPhotoPreview, setNewPhotoPreview]   = useState(null)
	const [newPhotoUrl, setNewPhotoUrl]           = useState(null)
	const [newPhotoUploading, setNewPhotoUploading] = useState(false)
	const [creating, setCreating]     = useState(false)
	const [formMsg, setFormMsg]       = useState(null)

	/* profile photo */
	const [photoFile, setPhotoFile]       = useState(null)
	const [photoPreview, setPhotoPreview] = useState(null)
	const [photoStatus, setPhotoStatus]   = useState(null)
	const [photoLoading, setPhotoLoading] = useState(false)
	const fileInputRef = useRef(null)

	/* ── auto-upload when admin picks a photo for the new user ── */
	useEffect(() => {
		if (!newPhotoFile) {
			setNewPhotoPreview(null)
			setNewPhotoUrl(null)
			return
		}
		setNewPhotoPreview(URL.createObjectURL(newPhotoFile))
		let cancelled = false
		async function upload() {
			setNewPhotoUploading(true)
			try {
				const fd = new FormData()
				fd.append('image', newPhotoFile)
				const { data } = await api.post('/admin/upload-image', fd)
				if (!cancelled) setNewPhotoUrl(data.profileImageUrl ?? null)
			} catch {
				if (!cancelled) setNewPhotoUrl(null)
			} finally {
				if (!cancelled) setNewPhotoUploading(false)
			}
		}
		upload()
		return () => { cancelled = true }
	}, [newPhotoFile])

	/* ── load ── */
	const loadAll = useCallback(async () => {
		setLoadError('')
		try {
			const [pRes, uRes, polRes, statsRes, disRes, finRes] = await Promise.all([
				api.get('/admin/pending-registrations'),
				api.get('/admin/users'),
				api.get('/admin/policies'),
				api.get('/admin/stats'),
				api.get('/admin/disruptions'),
				api.get('/admin/finance'),
			])
			setPending(pRes.data ?? [])
			setUsers(uRes.data ?? [])
			setPolicies(polRes.data ?? [])
			setStats(statsRes.data ?? null)
			setDisruptions(disRes.data ?? [])
			setFinance(finRes.data ?? null)
		} catch (e) {
			setLoadError(parseApiError(e.response?.data))
		}
	}, [])

	const loadDisruptions = useCallback(async () => {
		try {
			const res = await api.get('/admin/disruptions')
			setDisruptions(res.data ?? [])
		} catch {}
	}, [])

	useEffect(() => { loadAll() }, [loadAll])
	useEffect(() => {
		const t = setInterval(loadAll, 5000)
		return () => clearInterval(t)
	}, [loadAll])

	async function onSavePolicy(id, body) {
		await api.put(`/admin/policies/${id}`, body)
		const res = await api.get('/admin/policies')
		setPolicies(res.data ?? [])
	}

	async function onSendOrder(workerId) {
		const res = await api.post(`/admin/send-order/${workerId}`)
		return res.data
	}

	async function onTriggerDisruption(workerId, type, severity) {
		const res = await api.post(`/admin/disruption/${workerId}`, { type, severity })
		await loadAll()
		return res.data
	}

	async function onBroadcastDisruption(city, type, severity) {
		const res = await api.post('/admin/disruption', { type, severity, location: city })
		await loadAll()
		return res.data
	}

	async function onSendToNearest(lat, lon, radiusKm) {
		const res = await api.post(
			`/admin/send-order/nearest?lat=${lat}&lon=${lon}&radiusKm=${radiusKm}`)
		await loadAll()
		return res.data
	}

	async function approve(id) {
		setBusyId(id)
		try { await api.post(`/admin/pending-registrations/${id}/approve`); await loadAll() }
		catch (e) { alert(parseApiError(e.response?.data)) }
		finally { setBusyId(null) }
	}
	async function reject(id) {
		if (!window.confirm('Reject this registration?')) return
		setBusyId(id)
		try { await api.post(`/admin/pending-registrations/${id}/reject`); await loadAll() }
		catch (e) { alert(parseApiError(e.response?.data)) }
		finally { setBusyId(null) }
	}
	async function removeUser(id) {
		if (!window.confirm('Delete this user permanently?')) return
		setBusyId(`u-${id}`)
		try { await api.delete(`/admin/users/${id}`); await loadAll() }
		catch (e) { alert(parseApiError(e.response?.data)) }
		finally { setBusyId(null) }
	}
	async function handleCreateUser(e) {
		e.preventDefault(); setFormMsg(null); setCreating(true)
		try {
			await api.post('/admin/create-user', {
				name: newName, email: newEmail, password: newPassword,
				role: newRole, location: newLocation || null,
				profileImageUrl: newPhotoUrl || null,
			})
			setFormMsg({ type: 'ok', text: `User "${newName}" created successfully.` })
			setNewName(''); setNewEmail(''); setNewPassword('')
			setNewLocation(''); setNewRole('WORKER')
			setNewPhotoFile(null); setNewPhotoPreview(null); setNewPhotoUrl(null)
			await loadAll()
		} catch (e) { setFormMsg({ type: 'err', text: parseApiError(e.response?.data) }) }
		finally { setCreating(false) }
	}
	function handlePhotoSelect(e) {
		const f = e.target.files?.[0] ?? null
		setPhotoFile(f); setPhotoStatus(null)
		setPhotoPreview(f ? URL.createObjectURL(f) : null)
	}
	async function handlePhotoUpload(e) {
		e.preventDefault()
		if (!photoFile) { setPhotoStatus({ type: 'err', text: 'Select an image first.' }); return }
		setPhotoLoading(true); setPhotoStatus(null)
		try {
			await uploadProfileImage(photoFile)
			setPhotoFile(null); setPhotoPreview(null)
			if (fileInputRef.current) fileInputRef.current.value = ''
			setPhotoStatus({ type: 'ok', text: 'Profile photo updated.' })
		} catch (err) {
			const d = err.response?.data
			setPhotoStatus({ type: 'err', text: (typeof d === 'object' && d?.error ? d.error : 'Upload failed.') })
		} finally { setPhotoLoading(false) }
	}

	const sharedUserProps = {
		users, pending, loadError, busyId, approve, reject, removeUser, handleCreateUser, loadAll,
		newName, setNewName, newEmail, setNewEmail, newPassword, setNewPassword,
		newRole, setNewRole, newLocation, setNewLocation,
		newPhotoPreview, newPhotoFile, setNewPhotoFile, newPhotoUploading,
		creating, formMsg,
	}

	return (
		<AdminShell activeTab={activeTab} onTabChange={setActiveTab}>
			{activeTab === 'dashboard'   && <TabDashboard users={users} pending={pending} loadError={loadError} stats={stats} />}
			{activeTab === 'users'       && <TabUsers {...sharedUserProps} />}
			{activeTab === 'policies'    && <TabPolicies policies={policies} onSavePolicy={onSavePolicy} />}
			{activeTab === 'finance'     && <TabFinance finance={finance} loadError={loadError} />}
			{activeTab === 'disruptions' && (
				<TabDisruptions
					users={users}
					disruptions={disruptions}
					loadDisruptions={loadDisruptions}
					onSendOrder={onSendOrder}
					onTriggerDisruption={onTriggerDisruption}
					onBroadcastDisruption={onBroadcastDisruption}
					onSendToNearest={onSendToNearest}
				/>
			)}
			{activeTab === 'profile'     && (
				<TabProfile
					user={user}
					photoPreview={photoPreview}
					fileInputRef={fileInputRef}
					handlePhotoSelect={handlePhotoSelect}
					handlePhotoUpload={handlePhotoUpload}
					photoLoading={photoLoading}
					photoStatus={photoStatus}
				/>
			)}
		</AdminShell>
	)
}
