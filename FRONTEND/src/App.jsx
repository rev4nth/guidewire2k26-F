import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './context/AuthContext'
import AdminDashboard from './pages/AdminDashboard'
import GovtDashboard from './pages/GovtDashboard'
import Login from './pages/Login'
import Register from './pages/Register'
import WorkerDashboard from './pages/WorkerDashboard'

function HomeRedirect() {
	const { token, user } = useAuth()
	if (!token || !user?.role) {
		return <Navigate to="/login" replace />
	}
	switch (user.role) {
		case 'ADMIN':
			return <Navigate to="/admin" replace />
		case 'WORKER':
			return <Navigate to="/worker" replace />
		case 'GOVT':
			return <Navigate to="/govt" replace />
		default:
			return <Navigate to="/login" replace />
	}
}

export default function App() {
	return (
		<Routes>
			<Route path="/" element={<HomeRedirect />} />
			<Route path="/login" element={<Login />} />
			<Route path="/register" element={<Register />} />
			<Route
				path="/admin"
				element={
					<ProtectedRoute roles={['ADMIN']}>
						<AdminDashboard />
					</ProtectedRoute>
				}
			/>
			<Route
				path="/worker"
				element={
					<ProtectedRoute roles={['WORKER']}>
						<WorkerDashboard />
					</ProtectedRoute>
				}
			/>
			<Route
				path="/govt"
				element={
					<ProtectedRoute roles={['GOVT']}>
						<GovtDashboard />
					</ProtectedRoute>
				}
			/>
			<Route path="*" element={<Navigate to="/" replace />} />
		</Routes>
	)
}
