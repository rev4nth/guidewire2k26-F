import { useCallback, useEffect, useState } from 'react'
import api from '../api/api'
import GovtShell from '../components/GovtShell'
import { proofImageDisplayUrl } from '../utils/proofImageUrl'

function Card({ children, className = '' }) {
	return <div className={`rounded-xl bg-white shadow-sm ring-1 ring-slate-200/80 ${className}`}>{children}</div>
}

function StatCard({ label, value, accent, icon }) {
	return (
		<div className="flex items-center gap-4 rounded-xl bg-white p-5 shadow-sm ring-1 ring-slate-200/80 transition hover:shadow-md">
			<div className={`flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl ${accent}`}>{icon}</div>
			<div>
				<p className="text-2xl font-bold text-slate-800">{value}</p>
				<p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
			</div>
		</div>
	)
}

const CLAIM_BADGE = {
	APPROVED: 'bg-emerald-100 text-emerald-800',
	PENDING_PROOF: 'bg-amber-100 text-amber-950',
	REJECTED: 'bg-red-100 text-red-800',
}

const SEV_BADGE = {
	LOW: 'bg-yellow-100 text-yellow-900 ring-1 ring-yellow-200',
	MEDIUM: 'bg-orange-100 text-orange-900 ring-1 ring-orange-200',
	HIGH: 'bg-red-100 text-red-900 ring-1 ring-red-200',
}

function TabDashboard({ stats, loadError }) {
	if (loadError) {
		return <div className="rounded-lg bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">{loadError}</div>
	}
	if (!stats) return <p className="text-sm text-slate-500">Loading…</p>
	const td = Number(stats.totalDisruptions ?? 0)
	const tc = Number(stats.totalClaims ?? 0)
	const tw = Number(stats.totalWorkersAffected ?? 0)
	return (
		<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
			<StatCard
				label="Total disruptions"
				value={td.toLocaleString()}
				accent="bg-indigo-50"
				icon={
					<svg className="h-6 w-6 text-indigo-600" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
						<path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
					</svg>
				}
			/>
			<StatCard
				label="Claims generated"
				value={tc.toLocaleString()}
				accent="bg-amber-50"
				icon={
					<svg className="h-6 w-6 text-amber-600" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
						<path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" />
					</svg>
				}
			/>
			<StatCard
				label="Workers affected"
				value={tw.toLocaleString()}
				accent="bg-emerald-50"
				icon={
					<svg className="h-6 w-6 text-emerald-600" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
						<path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0z" />
					</svg>
				}
			/>
		</div>
	)
}

function TabDeclare({ dtype, setDtype, dsev, setDsev, dcity, setDcity, onSubmit, busy, msg }) {
	return (
		<Card className="p-6 max-w-lg">
			<h2 className="text-sm font-semibold text-slate-800">Declare official disruption</h2>
			<p className="mt-1 text-xs text-slate-500">
				Triggers the same workflow as platform disruptions: affected workers with active orders receive claims. Source is recorded
				as <span className="font-semibold text-emerald-700">GOVT</span>.
			</p>
			<form onSubmit={onSubmit} className="mt-5 space-y-4">
				<div>
					<label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Type</label>
					<select
						value={dtype}
						onChange={(e) => setDtype(e.target.value)}
						className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
					>
						<option value="BANDH">BANDH</option>
						<option value="FLOOD">FLOOD</option>
						<option value="RAIN">RAIN</option>
					</select>
				</div>
				<div>
					<label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Severity</label>
					<select
						value={dsev}
						onChange={(e) => setDsev(e.target.value)}
						className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
					>
						<option value="HIGH">HIGH</option>
						<option value="MEDIUM">MEDIUM</option>
						<option value="LOW">LOW</option>
					</select>
				</div>
				<div>
					<label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Location (city)</label>
					<input
						required
						value={dcity}
						onChange={(e) => setDcity(e.target.value)}
						placeholder="e.g. Hyderabad"
						className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
					/>
				</div>
				{msg && (
					<p className={`text-sm font-medium ${msg.type === 'ok' ? 'text-emerald-600' : 'text-red-600'}`}>{msg.text}</p>
				)}
				<button
					type="submit"
					disabled={busy}
					className="w-full rounded-lg bg-emerald-600 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700 disabled:opacity-50"
				>
					{busy ? 'Declaring…' : 'Declare disruption'}
				</button>
			</form>
		</Card>
	)
}

function TabClaimsTable({ claims, onVerify, verifyBusy }) {
	return (
		<Card className="overflow-hidden">
			<div className="border-b border-slate-100 px-5 py-4">
				<h2 className="text-sm font-semibold text-slate-800">All claims</h2>
				<p className="text-xs text-slate-500">Approve or reject pending reviews. Approval credits the worker wallet if not yet paid.</p>
			</div>
			{claims.length === 0 ? (
				<p className="py-12 text-center text-sm text-slate-400">No claims in the system</p>
			) : (
				<div className="overflow-x-auto">
					<table className="min-w-full text-sm">
						<thead>
							<tr className="border-b border-slate-100 bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
								<th className="px-4 py-3">ID</th>
								<th className="px-4 py-3">Worker</th>
								<th className="px-4 py-3">Severity</th>
								<th className="px-4 py-3 text-right">Amount</th>
								<th className="px-4 py-3">Confidence</th>
								<th className="px-4 py-3">Proof</th>
								<th className="px-4 py-3">Status</th>
								<th className="px-4 py-3 text-right">Actions</th>
							</tr>
						</thead>
						<tbody className="divide-y divide-slate-50">
							{claims.map((c) => {
								const done = c.status === 'APPROVED' || c.status === 'REJECTED'
								const busy = verifyBusy === c.claimId
								return (
									<tr key={c.claimId} className="hover:bg-slate-50/80">
										<td className="px-4 py-3 font-mono text-xs text-slate-400">#{c.claimId}</td>
										<td className="px-4 py-3 font-medium text-slate-800">{c.workerName}</td>
										<td className="px-4 py-3">
											{c.disruptionSeverity ? (
												<span
													className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${
														SEV_BADGE[c.disruptionSeverity] ?? 'bg-slate-100 text-slate-700'
													}`}
												>
													{c.disruptionSeverity}
												</span>
											) : (
												<span className="text-xs text-slate-400">—</span>
											)}
										</td>
										<td className="px-4 py-3 text-right font-semibold text-slate-800">₹{Number(c.amount).toLocaleString()}</td>
										<td className="px-4 py-3 text-slate-600">{c.confidenceScore ?? 0}/100</td>
										<td className="px-4 py-3 max-w-[8rem]">
											{c.proofImageUrl ? (
												<a
													href={proofImageDisplayUrl(c.proofImageUrl)}
													target="_blank"
													rel="noreferrer"
													className="text-xs font-medium text-emerald-700 hover:underline"
												>
													View proof
												</a>
											) : (
												<span className="text-xs text-slate-400">—</span>
											)}
										</td>
										<td className="px-4 py-3">
											<span
												className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${
													CLAIM_BADGE[c.status] ?? 'bg-slate-100 text-slate-700'
												}`}
											>
												{c.status}
											</span>
										</td>
										<td className="px-4 py-3 text-right whitespace-nowrap">
											{done ? (
												<span className="text-xs text-slate-400">—</span>
											) : (
												<div className="flex flex-wrap justify-end gap-2">
													<button
														type="button"
														disabled={busy}
														onClick={() => onVerify(c.claimId, 'APPROVED')}
														className="rounded-md bg-emerald-600 px-3 py-1 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
													>
														{busy ? '…' : 'Approve'}
													</button>
													<button
														type="button"
														disabled={busy}
														onClick={() => onVerify(c.claimId, 'REJECTED')}
														className="rounded-md border border-red-200 bg-red-50 px-3 py-1 text-xs font-semibold text-red-700 hover:bg-red-100 disabled:opacity-50"
													>
														Reject
													</button>
												</div>
											)}
										</td>
									</tr>
								)
							})}
						</tbody>
					</table>
				</div>
			)}
		</Card>
	)
}

function TabStatsVisual({ stats, loadError }) {
	if (loadError) {
		return <div className="rounded-lg bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">{loadError}</div>
	}
	if (!stats) return <p className="text-sm text-slate-500">Loading…</p>
	const td = Number(stats.totalDisruptions ?? 0)
	const tc = Number(stats.totalClaims ?? 0)
	const tw = Number(stats.totalWorkersAffected ?? 0)
	const max = Math.max(td, tc, tw, 1)
	const rows = [
		{ label: 'Total disruptions', value: td, color: 'bg-indigo-500' },
		{ label: 'Total claims', value: tc, color: 'bg-amber-500' },
		{ label: 'Workers affected (distinct)', value: tw, color: 'bg-emerald-500' },
	]
	return (
		<div className="space-y-6">
			<div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
				{rows.map((r) => (
					<Card key={r.label} className="p-4">
						<p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{r.label}</p>
						<p className="mt-1 text-2xl font-bold text-slate-800">{r.value.toLocaleString()}</p>
					</Card>
				))}
			</div>
			<Card className="p-6">
				<h3 className="text-sm font-semibold text-slate-800">Relative scale</h3>
				<p className="mt-0.5 text-xs text-slate-500">Bars normalized to the largest metric above.</p>
				<div className="mt-4 space-y-4">
					{rows.map((r) => (
						<div key={r.label}>
							<div className="mb-1 flex justify-between text-xs text-slate-600">
								<span>{r.label}</span>
								<span className="font-mono">{r.value}</span>
							</div>
							<div className="h-3 w-full overflow-hidden rounded-full bg-slate-100">
								<div
									className={`h-full rounded-full ${r.color} transition-all`}
									style={{ width: `${Math.round((r.value / max) * 100)}%` }}
								/>
							</div>
						</div>
					))}
				</div>
			</Card>
		</div>
	)
}

export default function GovtDashboard() {
	const [activeTab, setActiveTab] = useState('dashboard')
	const [stats, setStats] = useState(null)
	const [claims, setClaims] = useState([])
	const [loadError, setLoadError] = useState('')
	const [declareBusy, setDeclareBusy] = useState(false)
	const [declareMsg, setDeclareMsg] = useState(null)
	const [verifyBusy, setVerifyBusy] = useState(null)

	const [dtype, setDtype] = useState('BANDH')
	const [dsev, setDsev] = useState('HIGH')
	const [dcity, setDcity] = useState('')

	const loadAll = useCallback(async () => {
		setLoadError('')
		try {
			const [sRes, cRes] = await Promise.all([api.get('/govt/stats'), api.get('/govt/claims')])
			setStats(sRes.data ?? null)
			setClaims(cRes.data ?? [])
		} catch (e) {
			const err = e.response?.data
			const text =
				typeof err === 'object' && err?.status === 'FAILED' && err?.error
					? err.error
					: typeof err === 'object' && err?.error
						? err.error
						: e.safeflexMessage
							? e.safeflexMessage
							: e.response?.status === 403
								? 'Forbidden'
								: 'Failed to load data'
			setLoadError(text)
		}
	}, [])

	useEffect(() => {
		loadAll()
	}, [loadAll])

	useEffect(() => {
		const t = setInterval(loadAll, 8000)
		return () => clearInterval(t)
	}, [loadAll])

	async function handleDeclare(e) {
		e.preventDefault()
		if (!dcity.trim()) return
		setDeclareBusy(true)
		setDeclareMsg(null)
		try {
			const { data } = await api.post('/govt/disruption', {
				type: dtype,
				severity: dsev,
				location: dcity.trim(),
			})
			const n = data?.affectedCount ?? 0
			setDeclareMsg({ type: 'ok', text: `Declared. ${n} worker(s) in scope.` })
			await loadAll()
		} catch (err) {
			const d = err.response?.data
			const msg =
				typeof d === 'object' && d?.status === 'FAILED' && d?.error
					? d.error
					: typeof d === 'object' && d?.error
						? d.error
						: err.safeflexMessage ?? 'Declaration failed'
			setDeclareMsg({ type: 'err', text: msg })
		} finally {
			setDeclareBusy(false)
		}
	}

	async function handleVerify(claimId, status) {
		setVerifyBusy(claimId)
		try {
			await api.post(`/govt/claim/${claimId}/verify`, { status })
			await loadAll()
		} catch (err) {
			const d = err.response?.data
			const msg =
				typeof d === 'object' && d?.status === 'FAILED' && d?.error
					? d.error
					: typeof d === 'object' && d?.error
						? d.error
						: err.safeflexMessage ?? 'Verification failed'
			alert(msg)
		} finally {
			setVerifyBusy(null)
		}
	}

	return (
		<GovtShell activeTab={activeTab} onTabChange={setActiveTab}>
			{activeTab === 'dashboard' && <TabDashboard stats={stats} loadError={loadError} />}
			{activeTab === 'disruptions' && (
				<TabDeclare
					dtype={dtype}
					setDtype={setDtype}
					dsev={dsev}
					setDsev={setDsev}
					dcity={dcity}
					setDcity={setDcity}
					onSubmit={handleDeclare}
					busy={declareBusy}
					msg={declareMsg}
				/>
			)}
			{activeTab === 'claims' && (
				<TabClaimsTable claims={claims} onVerify={handleVerify} verifyBusy={verifyBusy} />
			)}
			{activeTab === 'stats' && <TabStatsVisual stats={stats} loadError={loadError} />}
		</GovtShell>
	)
}
