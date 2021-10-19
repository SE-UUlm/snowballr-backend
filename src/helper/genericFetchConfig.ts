import { HttpUserAgents } from "../api/httpUserAgent.ts";

/**
* Generate a config to imitate a user agent to minimize getting blocked.
*
* @param userAgent: allow to specify a user agent http header if u want to use the same for multiple requests
* @param lastRefererUrl: simulates redirects by passing this header
* @returns return a complete config for a "fetch" call. without cookies and client
*/
export const genericFetchConfig = (userAgent?: Object, lastRefererUrl?: string): Object => {
	return {
		params: {},
		headers: {
			'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9',
			'user-agent': userAgent ? userAgent : HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)],
			"accept-encoding": "gzip, deflate, br",
			"accept-language": "en-US,en;q=0.9,de;q=0.8",
			'referer': lastRefererUrl ? lastRefererUrl : 'https://www.google.com/',
			"sec-fetch-dest": "document",
			"sec-fetch-mode": "navigate",
			"sec-fetch-site": "same-origin",
			"sec-fetch-user": "?1",
			"upgrade-insecure-requests": 1
		}
	}
}