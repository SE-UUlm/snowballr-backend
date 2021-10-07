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

<user>   ALL=NOPASSWD:/usr/sbin/service tor

CHECK TOR IP:
curl --socks5 127.0.0.1:9050 http://checkip.amazonaws.com/

*/
export class Proxy {
	protected _userAgent: Object | undefined;
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
		//consoconsole.log(this._address)
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

	public async blocked(): Promise<void> {
		//this._cooldown = 200;
		this.onCooldown = true;
		this.isCurrentlyUsed = false;
		setTimeout(() => {
			this.onCooldown = false;
		}, this._cooldown)
	}

}

export class TorProxy extends Proxy {
	public async blocked(): Promise<void> {
		logger.warning("Tor Proxy got blocked. Reloading service to get new ip. Deno must be run as admin to do so!!!!!");
		this._userAgent = HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)];

		// https://stackoverflow.com/questions/62142699/how-do-i-run-an-arbitrary-shell-command-from-deno
		const process = Deno.run({
			cmd: ["sudo", "service", "tor", "reload"],
			stdout: "piped",
			stderr: "piped"
		});

		const debug = Deno.run({
			cmd: ["curl", "--socks5", "127.0.0.1:9050", "http://checkip.amazonaws.com/"],
			stdout: "piped",
			stderr: "piped"
		});
		const debugOut = await debug.output();
		const debugOutStr = new TextDecoder().decode(debugOut);
		console.log(debugOutStr);
		debug.close();

		const output = await process.output() // "piped" must be set
		const outStr = new TextDecoder().decode(output);
		console.log(outStr);


		const status: Deno.ProcessStatus = await process.status();
		if (status.success === false) {
			process.close();
			throw new Error("Error handling tor service. Run deno as admin and check for tor being installed.");
		}
		process.close();
	}
}

class ProxyManager {
	private static _instance: ProxyManager;
	private _proxies: Proxy[] = [] as Proxy[];
	private _settings: any;
	private _mode: "pool" | "tor";

	//allows to pass addresses and ports otherwise takes them from the env
	private constructor(proxyPorts?: number[], proxyAddresses?: number[]) {

		//console.log(data);
		try {
			var data: any = parse(Deno.readTextFileSync("../../config.yaml"));
			this._mode = data.googleScholar.proxy.mode;
		}
		catch (e) {
			throw new Error("Cannot read config.yml from project source. Or config.yml isn't formatted correctly. Error: " + e.message);
		}
		if (this._mode === "pool") {
			this._settings = data.googleScholar;
			for (let p in data.googleScholar.proxy.pool) {
				//console.log(p)
				let proxy = new Proxy(`${data.googleScholar.proxy.pool[p]}`)
				this._proxies.push(proxy);
			}
		}
		else if (this._mode === "tor") {
			this._proxies.push(new TorProxy(`${data.googleScholar.proxy.urls[0]}`));
		}
		else {
			throw new Error("Invalid config.yaml config for googleScholar")
		}
	}

	public static Instance(proxyPorts?: number[], proxyAddresses?: number[]) {
		// Do you need arguments? Make it a regular static method instead.
		return this._instance || (this._instance = new this(proxyPorts ? proxyPorts : undefined, proxyAddresses ? proxyAddresses : undefined));
	}

	public async acquire(): Promise<Proxy> {
		if (this._mode === "tor") {
			return this._proxies[0];
		}

		if (!this._settings.proxy.enabled) {
			logger.info(`GS: Proxy is disabled via config.yaml. Generating dummmy for agent simulation`)
			return new Proxy("", true);
		}
		console.debug("Trying to acquire Proxy");
		let proxy: Proxy = {} as Proxy;
		this._proxies.forEach((p: any) => {
			//console.log(p.isCurrentlyUsed)
			//console.log(p.onCooldown)
			if (!p.isCurrentlyUsed && !p.onCooldown) {
				p.isCurrentlyUsed = true;
				////console.log(p)
				proxy = p;
			}
		});
		await sleep(1);
		//console.log("waiting for a free proxy")
		return proxy;
		//}
	}

	public exchange(blockingProxy: Proxy): Promise<Proxy> | undefined {
		logger.debug("Trying to EXCHANGE Proxy")
		blockingProxy.blocked();
		return this.acquire();
	}

}

// Just create a singleton since this is a management instance.
export const proxyPool = ProxyManager.Instance();

