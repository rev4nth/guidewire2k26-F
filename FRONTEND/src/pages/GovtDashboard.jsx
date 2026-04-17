import Navbar from '../components/Navbar'
import ProfilePhotoForm from '../components/ProfilePhotoForm'

export default function GovtDashboard() {
	return (
		<div className="min-h-screen bg-slate-50">
			<Navbar />
			<main className="mx-auto max-w-5xl space-y-8 px-4 py-10 sm:px-6">
				<h1 className="text-2xl font-bold text-slate-900">Govt Panel</h1>
				<ProfilePhotoForm />
			</main>
		</div>
	)
}
