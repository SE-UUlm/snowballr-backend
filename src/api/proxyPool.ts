import { HttpUserAgents } from './httpUserAgent.ts';
import { parse } from "https://deno.land/std/encoding/yaml.ts";
import { logger } from "./logger.ts"
import { sleep } from "https://deno.land/x/sleep/mod.ts";


// install polipo
/*
apt-get install tor
sudo service tor start/stop

wget http://archive.ubuntu.com/ubuntu/pool/universe/p/polipo/polipo_1.1.1-8_amd64.deb
sudo dpkg -i polipo_1.1.1-8_amd64.deb

/etc/polipo/config:
socksParentProxy = localhost:9050
diskCacheRoot=""

*/
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
	public isWorking: boolean = true;
	public isDummy: boolean = false;

	public constructor(address: string, isDummy?: boolean, user?: string, password?: string) {
		this._userAgent = HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)];
		this._address = address;
		this._user = user ? user : undefined;
		this._password = password ? password : undefined;
		this.isDummy = isDummy ? isDummy : false;
	}

	public randomFetchConfig(lastRefererUrl?: string, currentCookie?: string): Object {
		let client = Deno.createHttpClient({})
		console.log(this._address)
		if (!this.isDummy) {
			client = Deno.createHttpClient({
				proxy: {
					url: this._address,
				}
			})
		}
		//console.log("PROXY: " + client)
		let config: any = {
			params: {},
			headers: {
				'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9',
				'user-agent': this._userAgent,
				"accept-encoding": "gzip, deflate, br",
				"accept-language": "en-US,en;q=0.9,de;q=0.8",
				'referer': lastRefererUrl ? lastRefererUrl : 'https://www.google.com/',
				"sec-fetch-dest": "document",
				"sec-fetch-mode": "navigate",
				"sec-fetch-site": "same-origin",
				"sec-fetch-user": "?1",
				"upgrade-insecure-requests": 1
			},
			client,

			//validateStatus: status) => true
		}
		if (currentCookie) { config.headers['cookie'] = currentCookie; }
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
	private _settings: any;

	//allows to pass addresses and ports otherwise takes them from the env
	private constructor(proxyPorts?: number[], proxyAddresses?: number[]) {
		const data: any = parse(Deno.readTextFileSync("../../config.yaml"));
		//console.log(data);
		this._settings = data.googleScholar;
		for (let p in data.googleScholar.proxy.pool) {
			console.log(p)
			let proxy = new Proxy(`${data.googleScholar.proxy.pool[p]}`)
			this._proxies.push(proxy);
		}
	}

	public static Instance(proxyPorts?: number[], proxyAddresses?: number[]) {
		// Do you need arguments? Make it a regular static method instead.
		return this._instance || (this._instance = new this(proxyPorts ? proxyPorts : undefined, proxyAddresses ? proxyAddresses : undefined));
	}

	public async acquire(): Promise<Proxy> {
		if (!this._settings.proxy.enabled) {
			logger.info(`GS: Proxy is disabled via config.yaml. Generating dummmy for agent simulation`)
			return new Proxy("", true);
		}
		console.debug("Trying to acquire Proxy");
		let proxy: Proxy = {} as Proxy;
		this._proxies.forEach((p: any) => {
			console.log(p.isCurrentlyUsed)
			console.log(p.onCooldown)
			if (!p.isCurrentlyUsed && !p.onCooldown) {
				p.isCurrentlyUsed = true;
				console.log(typeof p)
				console.log(p)
				proxy = p;
			}
		});
		await sleep(1);
		//console.log("waiting for a free proxy")
		return proxy;
		//}
	}

	public exchange(blockingProxy: Proxy): Promise<Proxy> | undefined {
		console.debug("Trying to EXCHANGE Proxy")
		blockingProxy.blocked();
		return this.acquire();
	}

}

// Just create a singleton since this is a management instance.
export const proxyPool = ProxyPool.Instance();

