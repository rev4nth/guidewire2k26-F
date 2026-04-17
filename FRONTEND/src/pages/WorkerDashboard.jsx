import { useCallback, useEffect, useRef, useState } from 'react'
import api, { uploadClaimProof } from '../api/api'
import { useGeoLocation } from '../hooks/useGeoLocation'
import WorkerShell from '../components/WorkerShell'
import { useAuth } from '../context/AuthContext'

/** Opens Razorpay’s checkout modal (visual only for your pitch — use test key via `VITE_RAZORPAY_KEY`). */
const RAZORPAY_KEY = import.meta.env.VITE_RAZORPAY_KEY || 'rzp_test_1DP5mmOlF5G5ag'

function openRazorpayCheckout(policy, onPaid, onDismiss) {
	if (typeof window.Razorpay !== 'function') {
		alert('Razorpay script did not load. Use “Buy policy (demo)” below — same outcome for the hackathon.')
		return false
	}
	const amountPaise = Math.round(Number(policy.premium) * 100)
	const rzp = new window.Razorpay({
		key: RAZORPAY_KEY,
		amount: amountPaise,
		currency: 'INR',
		name: 'SafeFlex',
		description: `Policy — ${policy.name}`,
		handler(response) {
			onPaid(response)
		},
		modal: { ondismiss: onDismiss },
	})
	rzp.open()
	return true
}

/* ──────── tiny shared UI ──────── */

function Card({ children, className = '' }) {
	return (
		<div className={`rounded-xl bg-white shadow-sm ring-1 ring-gray-100 ${className}`}>
			{children}
		</div>
	)
}

function StatCard({ label, value, accent, icon }) {
	return (
		<div className="flex items-center gap-4 rounded-xl bg-white p-5 shadow-sm ring-1 ring-gray-100 transition hover:shadow-md hover:-translate-y-0.5">
			<div className={`flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl ${accent}`}>{icon}</div>
			<div>
				<p className="text-2xl font-bold text-gray-800">{value}</p>
				<p className="text-xs text-gray-500">{label}</p>
			</div>
		</div>
	)
}

const STATUS_BADGE = {
	PENDING:   'bg-orange-100 text-orange-700',
	ACCEPTED:  'bg-blue-100 text-blue-700',
	PICKED_UP: 'bg-purple-100 text-purple-700',
	DELIVERED: 'bg-emerald-100 text-emerald-700',
	CANCELLED: 'bg-red-100 text-red-600',
}
function StatusBadge({ status }) {
	return (
		<span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${STATUS_BADGE[status] ?? 'bg-gray-100 text-gray-600'}`}>
			{status}
		</span>
	)
}

function DisruptionAlert({ disruptions }) {
	const latest = disruptions[0]
	if (!latest) return null
	const age = Date.now() - new Date(latest.createdAt).getTime()
	if (age > 5 * 60 * 1000) return null // only show if < 5 min old
	return (
		<div className="flex items-start gap-3 rounded-xl bg-red-50 p-4 ring-1 ring-red-200">
			<svg className="mt-0.5 h-5 w-5 flex-shrink-0 text-red-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
			</svg>
			<div>
				<p className="text-sm font-semibold text-red-700">
					⚠ Disruption detected: {latest.type} ({latest.severity})
				</p>
				<p className="text-xs font-medium text-red-600/90 mt-0.5">
					Source: {latest.source || 'MANUAL'}
				</p>
				<p className="text-xs text-red-500 mt-0.5">Your active order may have been cancelled.</p>
			</div>
		</div>
	)
}

function ClaimAlert({ claims }) {
	const latest = claims[0]
	if (!latest || latest.status !== 'APPROVED') return null
	const age = Date.now() - new Date(latest.createdAt).getTime()
	if (age > 5 * 60 * 1000) return null
	const bullets =
		latest.verificationBullets?.length > 0
			? latest.verificationBullets
			: (latest.reason || '')
					.split('\n')
					.map((s) => s.trim())
					.filter(Boolean)
	return (
		<div className="flex items-start gap-3 rounded-xl bg-emerald-50 p-4 ring-1 ring-emerald-200">
			<svg className="mt-0.5 h-5 w-5 flex-shrink-0 text-emerald-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
				<path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
			</svg>
			<div>
				<p className="text-sm font-semibold text-emerald-700">₹{Number(latest.amount).toLocaleString()} credited</p>
				<p className="mt-1 text-xs font-semibold text-emerald-800">Reason</p>
				<ul className="mt-1 space-y-0.5 text-xs text-emerald-800">
					{bullets.map((line) => (
						<li key={line}>{line}</li>
					))}
				</ul>
				<p className="text-xs text-emerald-600/90 mt-1.5">
					Confidence:{' '}
					<span className="font-semibold text-emerald-700">{latest.confidenceScore ?? 0}%</span>
				</p>
			</div>
		</div>
	)
}

/* ──────── Location Card ──────── */
function LocationCard({ coords, city, status, error, onRefresh }) {
	const statusConfig = {
		idle:       { color: 'text-gray-400',   dot: 'bg-gray-300',   label: 'Not started' },
		requesting: { color: 'text-blue-500',   dot: 'bg-blue-400 animate-pulse', label: 'Requesting…' },
		syncing:    { color: 'text-blue-500',   dot: 'bg-blue-400 animate-pulse', label: 'Syncing…' },
		ok:         { color: 'text-emerald-600',dot: 'bg-emerald-500', label: 'Location active' },
		error:      { color: 'text-red-500',    dot: 'bg-red-400',    label: 'Error' },
	}
	const cfg = statusConfig[status] ?? statusConfig.idle

	return (
		<Card className="p-5">
			<div className="flex items-start justify-between gap-2">
				<div className="flex items-center gap-2">
					<svg className="h-5 w-5 text-blue-500" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
						<path strokeLinecap="round" strokeLinejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" />
						<path strokeLinecap="round" strokeLinejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" />
					</svg>
					<p className="text-xs font-semibold uppercase tracking-wide text-gray-500">My Location</p>
				</div>
				<div className="flex items-center gap-1.5">
					<span className={`h-2 w-2 rounded-full flex-shrink-0 ${cfg.dot}`} />
					<span className={`text-xs font-medium ${cfg.color}`}>{cfg.label}</span>
				</div>
			</div>

			<div className="mt-3">
				{city ? (
					<p className="text-lg font-bold text-gray-800">{city}</p>
				) : (
					<p className="text-sm text-gray-400">City unknown</p>
				)}
				{coords && (
					<p className="mt-0.5 text-xs text-gray-400">
						{coords.lat.toFixed(5)}, {coords.lon.toFixed(5)}
					</p>
				)}
				{error && (
					<p className="mt-1.5 text-xs text-red-500">{error}</p>
				)}
			</div>

			{(status === 'idle' || status === 'error') && (
				<button
					type="button"
					onClick={onRefresh}
					className="mt-3 flex items-center gap-1.5 rounded-md bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-600 transition hover:bg-blue-100"
				>
					<svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
						<path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
					</svg>
					{status === 'error' ? 'Retry' : 'Share location'}
				</button>
			)}
		</Card>
	)
}

/* ──────── TAB: Dashboard ──────── */
function TabDashboard({ orders, claims, disruptions, geoLocation }) {
	const pending   = orders.filter((o) => o.status === 'PENDING').length
	const active    = orders.filter((o) => ['ACCEPTED', 'PICKED_UP'].includes(o.status)).length
	const delivered = orders.filter((o) => o.status === 'DELIVERED').length
	const lastApproved = claims.find((c) => c.status === 'APPROVED')
	const riskLevel = disruptions.length > 0 ? 'HIGH' : 'LOW'

	return (
		<div className="space-y-5">
			<DisruptionAlert disruptions={disruptions} />
			<ClaimAlert claims={claims} />

			<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
				<StatCard
					label="Pending orders"
					value={pending}
					accent="bg-orange-50"
					icon={<svg className="h-6 w-6 text-orange-400" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
				/>
				<StatCard
					label="Active orders"
					value={active}
					accent="bg-blue-50"
					icon={<svg className="h-6 w-6 text-blue-400" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M8.25 18.75a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 01-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 00-3.213-9.193 2.056 2.056 0 00-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 00-10.026 0 1.106 1.106 0 00-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12" /></svg>}
				/>
				<StatCard
					label="Last approved claim (₹)"
					value={lastApproved ? Number(lastApproved.amount).toLocaleString() : '—'}
					accent={riskLevel === 'HIGH' ? 'bg-red-50' : 'bg-emerald-50'}
					icon={
						<svg className={`h-6 w-6 ${riskLevel === 'HIGH' ? 'text-red-400' : 'text-emerald-400'}`} fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
							<path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
						</svg>
					}
				/>
			</div>

			<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
				<Card className="p-5">
					<p className="mb-1 text-xs font-semibold uppercase tracking-wide text-gray-400">Risk Level</p>
					<div className={`flex items-center gap-2 text-xl font-bold ${riskLevel === 'HIGH' ? 'text-red-600' : 'text-emerald-600'}`}>
						<span className={`h-3 w-3 rounded-full ${riskLevel === 'HIGH' ? 'bg-red-500' : 'bg-emerald-500'}`} />
						{riskLevel}
					</div>
					<p className="mt-1 text-xs text-gray-400">{disruptions.length} disruption(s) recorded</p>
				</Card>
				<Card className="p-5">
					<p className="mb-1 text-xs font-semibold uppercase tracking-wide text-gray-400">Deliveries</p>
					<p className="text-2xl font-bold text-gray-800">{delivered}</p>
					<p className="mt-1 text-xs text-gray-400">Orders delivered total</p>
				</Card>
				{geoLocation && (
					<LocationCard
						coords={geoLocation.coords}
						city={geoLocation.city}
						status={geoLocation.status}
						error={geoLocation.error}
						onRefresh={geoLocation.request}
					/>
				)}
			</div>
		</div>
	)
}

/* ──────── TAB: Orders ──────── */
function TabOrders({ orders, onAction, busy }) {
	const pending   = orders.filter((o) => o.status === 'PENDING')
	const active    = orders.filter((o) => ['ACCEPTED', 'PICKED_UP'].includes(o.status))
	const completed = orders.filter((o) => ['DELIVERED', 'CANCELLED'].includes(o.status))

	function ActionBtn({ label, onClick, color = 'blue', disabled }) {
		const colors = {
			blue:    'bg-blue-600 hover:bg-blue-700 text-white',
			green:   'bg-emerald-600 hover:bg-emerald-700 text-white',
			purple:  'bg-purple-600 hover:bg-purple-700 text-white',
			red:     'border border-red-200 bg-red-50 text-red-700 hover:bg-red-100',
		}
		return (
			<button
				type="button"
				disabled={disabled}
				onClick={onClick}
				className={`rounded-md px-3 py-1.5 text-xs font-semibold transition disabled:opacity-40 ${colors[color]}`}
			>
				{disabled ? '…' : label}
			</button>
		)
	}

	function OrderRow({ order }) {
		const isBusy = busy === order.id
		return (
			<tr className="transition-colors hover:bg-gray-50/70">
				<td className="px-5 py-3 font-mono text-xs text-gray-400">#{order.id}</td>
				<td className="px-5 py-3 text-xs text-gray-400">{new Date(order.createdAt).toLocaleString()}</td>
				<td className="px-5 py-3"><StatusBadge status={order.status} /></td>
				<td className="px-5 py-3 text-right">
					<div className="flex justify-end gap-2">
						{order.status === 'PENDING' && (
							<>
								<ActionBtn label="Accept" color="blue" disabled={isBusy} onClick={() => onAction(order.id, 'accept')} />
								<ActionBtn label="Cancel" color="red" disabled={isBusy} onClick={() => onAction(order.id, 'cancel')} />
							</>
						)}
						{order.status === 'ACCEPTED' && (
							<ActionBtn label="Picked Up" color="green" disabled={isBusy} onClick={() => onAction(order.id, 'pickup')} />
						)}
						{order.status === 'PICKED_UP' && (
							<ActionBtn label="Deliver" color="purple" disabled={isBusy} onClick={() => onAction(order.id, 'deliver')} />
						)}
					</div>
				</td>
			</tr>
		)
	}

	function Section({ title, rows, accent }) {
		return (
			<Card>
				<div className={`flex items-center justify-between border-b border-gray-100 px-6 py-4`}>
					<div className="flex items-center gap-2">
						<span className={`h-2.5 w-2.5 rounded-full ${accent}`} />
						<h2 className="text-sm font-semibold text-gray-800">{title}</h2>
						<span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-semibold text-gray-500">{rows.length}</span>
					</div>
				</div>
				{rows.length === 0 ? (
					<p className="py-8 text-center text-sm text-gray-400">No orders here</p>
				) : (
					<div className="overflow-x-auto">
						<table className="min-w-full text-sm">
							<thead>
								<tr className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
									<th className="px-5 py-3">Order ID</th>
									<th className="px-5 py-3">Received</th>
									<th className="px-5 py-3">Status</th>
									<th className="px-5 py-3 text-right">Actions</th>
								</tr>
							</thead>
							<tbody className="divide-y divide-gray-50">
								{rows.map((o) => <OrderRow key={o.id} order={o} />)}
							</tbody>
						</table>
					</div>
				)}
			</Card>
		)
	}

	return (
		<div className="space-y-6">
			<Section title="Pending Orders" rows={pending} accent="bg-orange-400" />
			<Section title="Active Orders" rows={active} accent="bg-blue-400" />
			<Section title="Completed / Cancelled" rows={completed} accent="bg-gray-400" />
		</div>
	)
}

/* ──────── TAB: Claims ──────── */
const CLAIM_STATUS_STYLE = {
	APPROVED: 'bg-emerald-100 text-emerald-800',
	REVIEW: 'bg-amber-100 text-amber-900',
	REJECTED: 'bg-red-100 text-red-800',
}

function TabClaims({ claims, disruptions, loadData }) {
	const [uploadBusy, setUploadBusy] = useState(null)
	const approvedSum = claims
		.filter((c) => c.status === 'APPROVED')
		.reduce((s, c) => s + Number(c.amount), 0)

	async function onProofFile(claimId, e) {
		const file = e.target.files?.[0]
		if (!file) return
		setUploadBusy(claimId)
		try {
			await uploadClaimProof(claimId, file)
			await loadData()
		} catch (err) {
			alert(err.message || 'Upload failed')
		} finally {
			setUploadBusy(null)
			e.target.value = ''
		}
	}

	return (
		<div className="space-y-6">
			<DisruptionAlert disruptions={disruptions} />
			<p className="rounded-lg bg-blue-50 px-4 py-2 text-sm text-blue-800 ring-1 ring-blue-100">
				Claims are scored using your active order, location vs disruption, and optional photo proof (max 100). Score
				≥70 approves payout to your <strong>wallet</strong>.
			</p>
			<Card>
				<div className="flex flex-col gap-1 border-b border-gray-100 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
					<h2 className="text-sm font-semibold text-gray-800">My Claims</h2>
					<span className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
						₹{approvedSum.toLocaleString()} paid (approved)
					</span>
				</div>
				{claims.length === 0 ? (
					<p className="py-12 text-center text-sm text-gray-400">No claims yet</p>
				) : (
					<div className="overflow-x-auto">
						<table className="min-w-full text-sm">
							<thead>
								<tr className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
									<th className="px-5 py-3">ID</th>
									<th className="px-5 py-3">Amount</th>
									<th className="px-5 py-3">Status</th>
									<th className="px-5 py-3">Confidence</th>
									<th className="px-5 py-3">Proof</th>
									<th className="px-5 py-3">Reason</th>
									<th className="px-5 py-3">Date</th>
								</tr>
							</thead>
							<tbody className="divide-y divide-gray-50">
								{claims.map((c) => (
									<tr key={c.id} className="hover:bg-gray-50/70">
										<td className="px-5 py-3 font-mono text-xs text-gray-400">#{c.id}</td>
										<td className="px-5 py-3 font-semibold text-gray-800">₹{Number(c.amount).toLocaleString()}</td>
										<td className="px-5 py-3">
											<span
												className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-bold ${
													CLAIM_STATUS_STYLE[c.status] ?? 'bg-gray-100 text-gray-600'
												}`}
											>
												{c.status ?? 'REVIEW'}
											</span>
										</td>
										<td className="px-5 py-3">
											<span
												className={`text-sm font-semibold ${
													c.status === 'APPROVED'
														? 'text-emerald-600'
														: c.status === 'REJECTED'
															? 'text-red-600'
															: 'text-amber-600'
												}`}
											>
												{c.confidenceScore ?? 0}%
											</span>
										</td>
										<td className="px-5 py-3">
											<div className="flex flex-col gap-1.5 min-w-[8rem]">
												{c.proofImage ? (
													<a
														href={c.proofImage}
														target="_blank"
														rel="noreferrer"
														className="text-xs font-medium text-blue-600 hover:underline truncate max-w-[10rem]"
													>
														View image
													</a>
												) : (
													<span className="text-xs text-gray-400">None</span>
												)}
												{c.status === 'REVIEW' && (
													<label className="cursor-pointer">
														<input
															type="file"
															accept="image/*"
															className="hidden"
															disabled={uploadBusy === c.id}
															onChange={(e) => onProofFile(c.id, e)}
														/>
														<span className="inline-block rounded-md bg-slate-100 px-2 py-1 text-[10px] font-semibold text-slate-700 hover:bg-slate-200">
															{uploadBusy === c.id ? 'Uploading…' : 'Upload proof'}
														</span>
													</label>
												)}
											</div>
										</td>
										<td className="px-5 py-3 text-gray-700 max-w-[14rem]">
											{c.status === 'APPROVED' && (
												<p className="mb-1 text-sm font-semibold text-emerald-700">
													₹{Number(c.amount).toLocaleString()} credited
												</p>
											)}
											<p className="text-xs font-semibold text-gray-500">Reason</p>
											<ul className="mt-0.5 space-y-0.5 text-xs text-gray-800">
												{(c.verificationBullets?.length ? c.verificationBullets : []).map((line) => (
													<li key={line}>{line}</li>
												))}
											</ul>
											{!(c.verificationBullets?.length > 0) && c.reason && (
												<p className="mt-1 text-xs text-gray-600 whitespace-pre-line">{c.reason}</p>
											)}
										</td>
										<td className="px-5 py-3 text-xs text-gray-400 whitespace-nowrap">
											{new Date(c.createdAt).toLocaleString()}
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

/* ──────── TAB: Policies ──────── */
const PLAN_ACCENT = {
	BASIC:   { bg: 'bg-blue-50', ring: 'ring-blue-200', btn: 'bg-blue-600 hover:bg-blue-700', badge: 'bg-blue-100 text-blue-700' },
	PRO:     { bg: 'bg-purple-50', ring: 'ring-purple-200', btn: 'bg-purple-600 hover:bg-purple-700', badge: 'bg-purple-100 text-purple-700' },
	PREMIUM: { bg: 'bg-amber-50', ring: 'ring-amber-200', btn: 'bg-amber-500 hover:bg-amber-600', badge: 'bg-amber-100 text-amber-700' },
}

function TabPolicies({ policies, user, refreshProfile, refreshWallet, buyBusy, setBuyBusy }) {
	async function buySimulated(p) {
		setBuyBusy(p.id)
		try {
			await api.post(`/worker/buy-policy/${p.id}`, { source: 'SIMULATE' })
			await refreshWallet()
			await refreshProfile()
			alert('Payment Successful ✅')
		} catch (e) {
			const msg = e.response?.data?.error ?? e.safeflexMessage ?? e.message ?? 'Purchase failed'
			alert(msg)
		} finally {
			setBuyBusy(null)
		}
	}

	async function buyWithWallet(p) {
		setBuyBusy(p.id)
		try {
			await api.post(`/worker/buy-policy/${p.id}`, { source: 'WALLET' })
			await refreshWallet()
			await refreshProfile()
			alert('Payment Successful ✅')
		} catch (e) {
			const msg = e.response?.data?.error ?? e.safeflexMessage ?? e.message ?? 'Purchase failed'
			alert(msg)
		} finally {
			setBuyBusy(null)
		}
	}

	function buyWithRazorpay(p) {
		setBuyBusy(p.id)
		const opened = openRazorpayCheckout(
			p,
			async (response) => {
				try {
					await api.post(`/worker/buy-policy/${p.id}`, {
						source: 'RAZORPAY',
						razorpayPaymentId: response.razorpay_payment_id || `rzp_demo_${Date.now()}`,
					})
					await refreshWallet()
					await refreshProfile()
					alert('Payment Successful ✅')
				} catch (e) {
					const msg = e.response?.data?.error ?? e.safeflexMessage ?? e.message ?? 'Could not confirm on server'
					alert(msg)
				} finally {
					setBuyBusy(null)
				}
			},
			() => setBuyBusy(null),
		)
		if (!opened) setBuyBusy(null)
	}

	return (
		<div className="space-y-4">
			<p className="text-sm text-gray-500">
				<span className="font-medium text-gray-700">Razorpay:</span> use the button below to show the real checkout UI (good for screenshots).
				<span className="font-medium text-gray-700"> Demo / wallet</span> hits the same safe backend. Wallet: ₹
				{Number(user?.walletBalance ?? 0).toLocaleString()}.
			</p>
			<div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
				{policies.filter((p) => p.active).map((p) => {
					const accent = PLAN_ACCENT[p.name.toUpperCase()] ?? PLAN_ACCENT.BASIC
					const isActive = user?.activePolicyId === p.id
					return (
						<div
							key={p.id}
							className={`flex flex-col rounded-2xl p-6 ring-1 shadow-sm transition hover:shadow-md ${accent.bg} ${accent.ring}`}
						>
							<div className="mb-2 flex flex-wrap items-center gap-2">
								<span className={`rounded-full px-3 py-1 text-xs font-bold ${accent.badge}`}>{p.name}</span>
								{isActive && (
									<span className="rounded-full bg-emerald-600 px-2 py-0.5 text-[10px] font-bold text-white">ACTIVE</span>
								)}
							</div>
							<p className="mb-1 text-3xl font-bold text-gray-900">₹{Number(p.premium).toLocaleString()}</p>
							<p className="mb-4 text-xs text-gray-500">per month</p>
							<div className="mb-4 flex items-center gap-2 rounded-lg bg-white/60 p-3">
								<svg className="h-5 w-5 text-emerald-500" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
									<path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
								</svg>
								<span className="text-sm font-semibold text-gray-700">₹{Number(p.coverage).toLocaleString()} coverage</span>
							</div>
							<div className="mt-auto flex flex-col gap-2">
								<button
									type="button"
									disabled={buyBusy === p.id || isActive}
									onClick={() => buyWithRazorpay(p)}
									className={`rounded-lg py-2.5 text-sm font-semibold text-white transition disabled:opacity-50 ${accent.btn}`}
								>
									{buyBusy === p.id ? 'Processing…' : 'Pay with Razorpay'}
								</button>
								<p className="text-center text-[10px] text-gray-500">Opens Razorpay checkout (for demo / slides)</p>
								<button
									type="button"
									disabled={buyBusy === p.id || isActive}
									onClick={() => buySimulated(p)}
									className="rounded-lg border border-gray-300 bg-white py-2 text-sm font-semibold text-gray-800 transition hover:bg-gray-50 disabled:opacity-50"
								>
									Buy policy (demo — no modal)
								</button>
								<button
									type="button"
									disabled={buyBusy === p.id || isActive}
									onClick={() => buyWithWallet(p)}
									className="rounded-lg border border-gray-300 bg-white py-2 text-sm font-semibold text-gray-800 transition hover:bg-gray-50 disabled:opacity-50"
								>
									Pay from wallet
								</button>
							</div>
						</div>
					)
				})}
			</div>
		</div>
	)
}

/* ──────── TAB: Wallet ──────── */
function TabWallet({ wallet, refreshWallet, refreshProfile }) {
	const [addAmt, setAddAmt] = useState('')
	const [wdAmt, setWdAmt] = useState('')
	const [msg, setMsg] = useState(null)
	const [busy, setBusy] = useState(false)

	async function addMoney(e) {
		e.preventDefault()
		const n = parseFloat(addAmt)
		if (!n || n <= 0) return
		setBusy(true)
		setMsg(null)
		try {
			await api.post('/worker/add-money', { amount: n })
			setAddAmt('')
			await refreshWallet()
			await refreshProfile()
			setMsg({ type: 'ok', text: 'Money added to wallet.' })
		} catch (err) {
			setMsg({ type: 'err', text: err.response?.data?.error ?? err.safeflexMessage ?? 'Failed to add money.' })
		} finally {
			setBusy(false)
		}
	}

	async function withdraw(e) {
		e.preventDefault()
		const n = parseFloat(wdAmt)
		if (!n || n <= 0) return
		setBusy(true)
		setMsg(null)
		try {
			await api.post('/worker/withdraw', { amount: n })
			setWdAmt('')
			await refreshWallet()
			await refreshProfile()
			setMsg({ type: 'ok', text: 'Withdrawal processed.' })
		} catch (err) {
			setMsg({ type: 'err', text: err.response?.data?.error ?? err.safeflexMessage ?? 'Withdraw failed.' })
		} finally {
			setBusy(false)
		}
	}

	const bal = wallet?.walletBalance ?? 0

	return (
		<div className="mx-auto max-w-lg space-y-5">
			<Card className="overflow-hidden p-0">
				<div className="bg-gradient-to-br from-blue-600 to-indigo-700 px-6 py-8 text-white">
					<p className="text-xs font-semibold uppercase tracking-wide text-blue-100">Wallet balance</p>
					<p className="mt-1 text-4xl font-bold">₹{Number(bal).toLocaleString()}</p>
				</div>
			</Card>

			{msg && (
				<p className={`rounded-lg px-3 py-2 text-sm font-medium ${msg.type === 'ok' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
					{msg.text}
				</p>
			)}

			<Card className="p-6">
				<h3 className="mb-3 text-sm font-semibold text-gray-800">Add money (demo)</h3>
				<form onSubmit={addMoney} className="flex flex-wrap gap-2">
					<input
						type="number"
						min="1"
						step="1"
						value={addAmt}
						onChange={(e) => setAddAmt(e.target.value)}
						placeholder="Amount"
						className="min-w-0 flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm"
					/>
					<button type="submit" disabled={busy} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50">
						Add
					</button>
				</form>
			</Card>

			<Card className="p-6">
				<h3 className="mb-3 text-sm font-semibold text-gray-800">Withdraw</h3>
				<form onSubmit={withdraw} className="flex flex-wrap gap-2">
					<input
						type="number"
						min="1"
						step="1"
						value={wdAmt}
						onChange={(e) => setWdAmt(e.target.value)}
						placeholder="Amount"
						className="min-w-0 flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm"
					/>
					<button type="submit" disabled={busy} className="rounded-md border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50">
						Withdraw
					</button>
				</form>
			</Card>
		</div>
	)
}

/* ──────── TAB: Profile ──────── */
function TabProfile({ user, refreshProfile }) {
	const [name, setName]     = useState(user?.name ?? '')
	const [location, setLocation] = useState(user?.location ?? '')
	const [photoFile, setPhotoFile] = useState(null)
	const [photoPreview, setPhotoPreview] = useState(null)
	const [uploading, setUploading] = useState(false)
	const [saving, setSaving] = useState(false)
	const [status, setStatus] = useState(null)
	const fileRef = useRef(null)

	function onFileChange(e) {
		const f = e.target.files?.[0]
		if (!f) return
		setPhotoFile(f)
		setPhotoPreview(URL.createObjectURL(f))
	}

	async function handleSave(e) {
		e.preventDefault()
		setSaving(true)
		setStatus(null)
		try {
			let profileImageUrl = user?.profileImageUrl ?? null
			if (photoFile) {
				setUploading(true)
				const fd = new FormData()
				fd.append('image', photoFile)
				const res = await api.post('/api/me/profile-image', fd)
				profileImageUrl = res.data?.profileImageUrl ?? profileImageUrl
				setUploading(false)
			}
			await api.put('/worker/profile', { name, location, profileImageUrl })
			await refreshProfile()
			setPhotoFile(null)
			setPhotoPreview(null)
			if (fileRef.current) fileRef.current.value = ''
			setStatus({ type: 'ok', text: 'Profile updated successfully.' })
		} catch {
			setUploading(false)
			setStatus({ type: 'err', text: 'Failed to save profile.' })
		} finally {
			setSaving(false)
		}
	}

	const currentPhoto = photoPreview ?? user?.profileImageUrl

	return (
		<div className="mx-auto max-w-lg space-y-5">
			<Card className="p-6">
				<h2 className="mb-5 text-sm font-semibold text-gray-800">Edit Profile</h2>
				<form onSubmit={handleSave} className="space-y-4">
					{/* Avatar */}
					<div className="flex flex-col items-center gap-3">
						{currentPhoto ? (
							<img src={currentPhoto} alt="" className="h-24 w-24 rounded-full border-4 border-white object-cover shadow-md" />
						) : (
							<div className="flex h-24 w-24 items-center justify-center rounded-full bg-blue-100 text-3xl font-bold text-blue-600 shadow-md">
								{(user?.name ?? 'W')[0].toUpperCase()}
							</div>
						)}
						<label className="flex cursor-pointer items-center gap-2 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50">
							<svg className="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
								<path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
							</svg>
							{photoFile ? photoFile.name : 'Upload photo'}
							<input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={onFileChange} />
						</label>
						{uploading && <p className="text-xs text-blue-500">Uploading photo…</p>}
					</div>

					{/* fields */}
					<div>
						<label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gray-500">Full name</label>
						<input
							value={name}
							onChange={(e) => setName(e.target.value)}
							className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
						/>
					</div>
					<div>
						<label className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-gray-500">Location</label>
						<input
							value={location}
							onChange={(e) => setLocation(e.target.value)}
							className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
							placeholder="City, Country"
						/>
					</div>

					{status && (
						<p className={`text-sm font-medium ${status.type === 'ok' ? 'text-emerald-600' : 'text-red-600'}`}>{status.text}</p>
					)}

					<button
						type="submit"
						disabled={saving}
						className="w-full rounded-lg bg-blue-600 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:opacity-50"
					>
						{saving ? 'Saving…' : 'Save Changes'}
					</button>
				</form>
			</Card>
		</div>
	)
}

/* ══════════════════ ROOT ══════════════════ */
export default function WorkerDashboard() {
	const { user, refreshProfile } = useAuth()
	const [activeTab, setActiveTab] = useState('dashboard')

	const [orders, setOrders]           = useState([])
	const [claims, setClaims]           = useState([])
	const [policies, setPolicies]       = useState([])
	const [disruptions, setDisruptions] = useState([])
	const [busy, setBusy]               = useState(null)
	const [wallet, setWallet]           = useState({ walletBalance: 0, activePolicyId: null })
	const [buyBusy, setBuyBusy]         = useState(null)
	const [claimPopup, setClaimPopup]   = useState(null)
	const seenClaimIdsRef = useRef(new Set())
	const claimBaselineDoneRef = useRef(false)
	const [dataReady, setDataReady] = useState(false)

	// GPS location — auto-requests on mount
	const geoLocation = useGeoLocation({ autoRequest: true })

	const refreshWallet = useCallback(async () => {
		try {
			const { data } = await api.get('/worker/wallet')
			setWallet({
				walletBalance: data.walletBalance ?? 0,
				activePolicyId: data.activePolicyId ?? null,
			})
			await refreshProfile()
		} catch { /* ignore */ }
	}, [refreshProfile])

	const loadData = useCallback(async () => {
		try {
			const [oRes, cRes, dRes, wRes] = await Promise.all([
				api.get('/worker/orders'),
				api.get('/worker/claims'),
				api.get('/worker/disruptions'),
				api.get('/worker/wallet'),
			])
			setOrders(oRes.data ?? [])
			setClaims(cRes.data ?? [])
			setDisruptions(dRes.data ?? [])
			const wd = wRes.data ?? {}
			setWallet({
				walletBalance: wd.walletBalance ?? 0,
				activePolicyId: wd.activePolicyId ?? null,
			})
		} catch { /* silent poll failure */ }
		finally {
			setDataReady(true)
		}
	}, [])

	const loadPolicies = useCallback(async () => {
		try {
			const res = await api.get('/policies')
			setPolicies(res.data ?? [])
		} catch { /* ignore */ }
	}, [])

	useEffect(() => {
		loadData()
		loadPolicies()
	}, [loadData, loadPolicies])

	useEffect(() => {
		const t = setInterval(loadData, 3500)
		return () => clearInterval(t)
	}, [loadData])

	useEffect(() => {
		if (!dataReady) return
		if (!claimBaselineDoneRef.current) {
			claims.forEach((c) => seenClaimIdsRef.current.add(c.id))
			claimBaselineDoneRef.current = true
			return
		}
		let t1
		let t2
		for (const c of claims) {
			if (seenClaimIdsRef.current.has(c.id)) continue
			seenClaimIdsRef.current.add(c.id)
			setClaimPopup({ phase: 'processing' })
			t1 = setTimeout(() => {
				if (c.status === 'APPROVED') {
					setClaimPopup({ phase: 'done', amount: c.amount })
				} else {
					setClaimPopup({ phase: 'review' })
				}
			}, 1000)
			t2 = setTimeout(() => setClaimPopup(null), 9000)
			break
		}
		return () => {
			if (t1) clearTimeout(t1)
			if (t2) clearTimeout(t2)
		}
	}, [claims, dataReady])

	async function onAction(orderId, action) {
		setBusy(orderId)
		try {
			await api.post(`/worker/orders/${orderId}/${action}`)
			await loadData()
		} catch (err) {
			alert(err.response?.data?.error ?? err.safeflexMessage ?? 'Action failed')
		} finally {
			setBusy(null)
		}
	}

	const pendingCount = orders.filter((o) => o.status === 'PENDING').length

	return (
		<WorkerShell activeTab={activeTab} onTabChange={setActiveTab} pendingCount={pendingCount}>
			{claimPopup && (
				<div
					role="dialog"
					aria-live="polite"
					className="fixed inset-0 z-[100] flex items-center justify-center bg-black/45 p-4"
				>
					<div className="w-full max-w-sm rounded-2xl bg-white px-6 py-8 text-center shadow-2xl ring-1 ring-gray-200">
						{claimPopup.phase === 'processing' && (
							<>
								<div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
								<p className="text-lg font-semibold text-gray-800">Processing claim…</p>
								<p className="mt-1 text-sm text-gray-500">Verifying your order and location</p>
							</>
						)}
						{claimPopup.phase === 'done' && (
							<>
								<p className="text-2xl font-bold text-emerald-600">
									₹{Number(claimPopup.amount).toLocaleString()} credited instantly 💰
								</p>
								<p className="mt-2 text-sm text-gray-600">Added to your wallet</p>
							</>
						)}
						{claimPopup.phase === 'review' && (
							<>
								<p className="text-lg font-semibold text-amber-700">Claim recorded</p>
								<p className="mt-1 text-sm text-gray-600">Under review — check back soon</p>
							</>
						)}
					</div>
				</div>
			)}
			{activeTab === 'dashboard' && (
				<TabDashboard orders={orders} claims={claims} disruptions={disruptions} geoLocation={geoLocation} />
			)}
			{activeTab === 'orders' && (
				<TabOrders orders={orders} onAction={onAction} busy={busy} />
			)}
			{activeTab === 'claims' && (
				<TabClaims claims={claims} disruptions={disruptions} loadData={loadData} />
			)}
			{activeTab === 'wallet' && (
				<TabWallet wallet={wallet} refreshWallet={refreshWallet} refreshProfile={refreshProfile} />
			)}
			{activeTab === 'policies' && (
				<TabPolicies
					policies={policies}
					user={{ ...user, walletBalance: wallet.walletBalance, activePolicyId: wallet.activePolicyId }}
					refreshProfile={refreshProfile}
					refreshWallet={refreshWallet}
					buyBusy={buyBusy}
					setBuyBusy={setBuyBusy}
				/>
			)}
			{activeTab === 'profile' && (
				<TabProfile user={user} refreshProfile={refreshProfile} />
			)}
		</WorkerShell>
	)
}
