/**
 * Legacy demo uploads used https://demo.safeflex.local/... which never resolves in the browser.
 * Map those to a real placeholder so thumbnails and previews work without changing DB rows.
 */
export function proofImageDisplayUrl(url) {
	if (url == null || typeof url !== 'string') return url
	if (url.includes('demo.safeflex.local')) {
		let h = 2166136261
		for (let i = 0; i < url.length; i++) {
			h = Math.imul(h ^ url.charCodeAt(i), 16777619)
		}
		return `https://picsum.photos/seed/${String(Math.abs(h))}/480/360`
	}
	return url
}
