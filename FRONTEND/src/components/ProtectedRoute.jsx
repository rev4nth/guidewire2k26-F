import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function homePathForRole(role) {
	switch (role) {
		case 'ADMIN':
			return '/admin'
		case 'WORKER':
			return '/worker'
		case 'GOVT':
			return '/govt'
		default:
			return '/login'
	}
}

export default function ProtectedRoute({ children, roles }) {
	const { token, user } = useAuth()
	const location = useLocation()

	if (!token) {
		return <Navigate to="/login" state={{ from: location }} replace />
	}

	if (roles?.length && user?.role && !roles.includes(user.role)) {
		return <Navigate to={homePathForRole(user.role)} replace />
	}

	return children
}
