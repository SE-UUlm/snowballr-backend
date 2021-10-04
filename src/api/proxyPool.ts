import { HttpUserAgents } from './httpUserAgent.ts';
import { parse } from "https://deno.land/std/encoding/yaml.ts";
import { logger } from "./logger.ts";

export class Proxy {
	private _userAgent: Object | undefined;
	private _lastRefererUrl: string | undefined;
	private _currentCookie: string | undefined;
	private _cooldown: number = 300;
	private _address: string;
	private _user: string | undefined;
	private _password: string | undefined;
	public onCooldown: boolean = false;
	public isCurrentlyUsed: boolean = false;

	public constructor(address: string, user?: string, password?: string) {
		this._userAgent = HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)];
		this._address = address;
		this._user = user ? user : undefined;
		this._password = password ? password : undefined;
	}

	public randomFetchConfig(): Object {
		let config: any = {
			params: {},
			headers: {
				'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9',
				'user-agent': this._userAgent,
				"accept-encoding": "gzip, deflate, br",
				"accept-language": "en-US,en;q=0.9,de;q=0.8",
				'referer': this._lastRefererUrl ? this._lastRefererUrl : 'https://www.google.com/',
				"sec-fetch-dest": "document",
				"sec-fetch-mode": "navigate",
				"sec-fetch-site": "same-origin",
				"sec-fetch-user": "?1",
				"upgrade-insecure-requests": 1
			},
			proxy: {
				url: this._address,
				basicAuth: {
					username: this._user,
					password: this._password
				}
			}
			//validateStatus: status) => true
		}
		if (this._currentCookie) { config.headers['cookie'] = this._currentCookie; }
		return config;
	}

	public blocked(): void {
		//this._cooldown = 200;
		this.onCooldown = true;
		this.isCurrentlyUsed = false;
		setTimeout(() => {
			this.onCooldown = false;
		}, this._cooldown)
	}

}

class ProxyPool {
	private static _instance: ProxyPool;
	private _proxies: Proxy[] = [] as Proxy[];

	//allows to pass addresses and ports otherwise takes them from the env
	private constructor(proxyPorts?: number[], proxyAddresses?: number[]) {
		const data: any = parse(Deno.readTextFileSync("../../config.yaml"));
		console.log(data);
		for (let a in data.src.api.proxy.addresses) {
			for (let p in data.src.api.proxy.ports) {
				let proxy = new Proxy(`${a}:${p}`)
				this._proxies.push(proxy);
			}
		}

	}

	public static Instance(proxyPorts?: number[], proxyAddresses?: number[]) {
		// Do you need arguments? Make it a regular static method instead.
		return this._instance || (this._instance = new this(proxyPorts ? proxyPorts : undefined, proxyAddresses ? proxyAddresses : undefined));
	}

	public acquire(): Proxy | undefined {
		console.debug("Trying to acquire Proxy")
		this._proxies.forEach((p: any) => {
			console.log(p.isCurrentlyUsed)
			console.log(p.onCooldown)
			if (!p.isCurrentlyUsed && !p.onCooldown) {
				console.log("here")
				p.isCurrentlyUsed = true;
				console.log("there")
				return p;
			}
		});
		logger.error('All proxies currently in use or blocked! You need to wait')
		return undefined;
	}

	public exchange(blockingProxy: Proxy): Proxy | undefined {
		console.debug("Trying to EXCHANGE Proxy")
		blockingProxy.blocked();
		return this.acquire();
	}

}

// Just create a singleton since this is a management instance.
export const proxyPool = ProxyPool.Instance();

