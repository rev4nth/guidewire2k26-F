import { useState } from 'react'
import { useAuth } from '../context/AuthContext'

export default function ProfilePhotoForm() {
	const { user, uploadProfileImage } = useAuth()
	const [file, setFile] = useState(null)
	const [status, setStatus] = useState('')
	const [loading, setLoading] = useState(false)

	async function handleSubmit(e) {
		e.preventDefault()
		if (!file) {
			setStatus('Choose an image first.')
			return
		}
		setStatus('')
		setLoading(true)
		try {
			await uploadProfileImage(file)
			setFile(null)
			setStatus('Profile photo saved.')
		} catch (err) {
			const d = err.response?.data
			const msg =
				typeof d === 'object' && d?.error
					? d.error
					: typeof d === 'string'
						? (() => {
								try {
									return JSON.parse(d).error
								} catch {
									return 'Upload failed'
								}
							})()
						: 'Upload failed'
			setStatus(msg)
		} finally {
			setLoading(false)
		}
	}

	return (
		<section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
			<h2 className="text-lg font-semibold text-slate-900">Profile photo</h2>
			<p className="mt-1 text-sm text-slate-500">
				Upload a profile image (stored on Cloudinary). Current:{' '}
				{user?.profileImageUrl ? (
					<a
						href={user.profileImageUrl}
						target="_blank"
						rel="noreferrer"
						className="font-medium text-sky-700 hover:underline"
					>
						view
					</a>
				) : (
					<span className="text-slate-400">none</span>
				)}
			</p>
			<form onSubmit={handleSubmit} className="mt-4 flex flex-wrap items-end gap-3">
				<input
					type="file"
					accept="image/*"
					onChange={(e) => {
						setFile(e.target.files?.[0] ?? null)
						setStatus('')
					}}
					className="max-w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-slate-100 file:px-3 file:py-2 file:text-sm file:font-medium file:text-slate-800 hover:file:bg-slate-200"
				/>
				<button
					type="submit"
					disabled={loading}
					className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-50"
				>
					{loading ? 'Uploading…' : 'Upload'}
				</button>
			</form>
			{status ? (
				<p
					className={`mt-3 text-sm ${status.includes('failed') || status.includes('Choose') ? 'text-red-600' : 'text-emerald-700'}`}
				>
					{status}
				</p>
			) : null}
		</section>
	)
}
