import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import api from '../api/api'

const AuthContext = createContext(null)

const TOKEN_KEY = 'safeflex_token'
const USER_KEY = 'safeflex_user'

function readStoredAuth() {
	const token = localStorage.getItem(TOKEN_KEY)
	const raw = localStorage.getItem(USER_KEY)
	let user = null
	if (raw) {
		try {
			user = JSON.parse(raw)
		} catch {
			localStorage.removeItem(USER_KEY)
		}
	}
	return { token, user }
}

export function AuthProvider({ children }) {
	const [{ token, user }, setState] = useState(() => {
		const { token: t, user: u } = readStoredAuth()
		return { token: t, user: u }
	})

	const login = useCallback(async (email, password) => {
		const { data } = await api.post('/auth/login', { email, password })
		const nextUser = {
			id: data.userId,
			role: data.role,
			name: data.name ?? null,
			email: data.email ?? null,
			profileImageUrl: data.profileImageUrl ?? null,
			walletBalance: data.walletBalance ?? null,
			activePolicyId: data.activePolicyId ?? null,
		}
		localStorage.setItem(TOKEN_KEY, data.token)
		localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
		setState({ token: data.token, user: nextUser })
		return data.role
	}, [])

	const refreshProfile = useCallback(async () => {
		const t = localStorage.getItem(TOKEN_KEY)
		if (!t) return null
		const { data } = await api.get('/api/me')
		const nextUser = {
			id: data.id,
			role: data.role,
			name: data.name ?? null,
			email: data.email ?? null,
			profileImageUrl: data.profileImageUrl ?? null,
			walletBalance: data.walletBalance ?? null,
			activePolicyId: data.activePolicyId ?? null,
		}
		localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
		setState((s) => ({ ...s, user: nextUser }))
		return nextUser
	}, [])

	const uploadProfileImage = useCallback(
		async (file) => {
			const fd = new FormData()
			fd.append('image', file)
			await api.post('/api/me/profile-image', fd)
			await refreshProfile()
		},
		[refreshProfile],
	)

	const logout = useCallback(() => {
		localStorage.removeItem(TOKEN_KEY)
		localStorage.removeItem(USER_KEY)
		setState({ token: null, user: null })
	}, [])

	const value = useMemo(
		() => ({
			token,
			user,
			login,
			logout,
			refreshProfile,
			uploadProfileImage,
		}),
		[token, user, login, logout, refreshProfile, uploadProfileImage],
	)

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
	const ctx = useContext(AuthContext)
	if (!ctx) {
		throw new Error('useAuth must be used within AuthProvider')
	}
	return ctx
}
