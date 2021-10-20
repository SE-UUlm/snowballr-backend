import { HttpUserAgents } from './httpUserAgent.ts';
import { logger } from "./logger.ts"
import { sleep } from "https://deno.land/x/sleep/mod.ts";
import { IProxy } from "./iProxy.ts"
import { genericFetchConfig } from "../helper/genericFetchConfig.ts";
import { CONFIG } from "../helper/config.ts";


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


DOCKER VERSION: 
docker run -d -p 8118:8118 -p 9050:9050 -p 8123:8123 --name snowballr_proxy simonpure/tor-privoxy-polipo
add to suders: <user> ALL = NOPASSWD: /usr/bin/docker restart snowballr_proxy

*/

/**
 * Base implementation of a proxy object able to return a proxied http_response
 *
 * @param address full address to the proxy. with protocol
 * @param user username for auth at the proxy if needed
 * @param password password for auth at the proxy if needed
 */
export class Proxy implements IProxy {
	protected _userAgent: Object | undefined;
	private _lastRefererUrl: string | undefined;
	private _currentCookie: string | undefined;
	protected _address: string;
	protected _user: string | undefined;
	protected _password: string | undefined;
	protected _activeClient: Deno.HttpClient | undefined;


	public constructor(address: string, user?: string, password?: string) {
		this._userAgent = HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)];
		this._address = address;
		this._user = user ? user : undefined;
		this._password = password ? password : undefined;
	}

	/**
	 * Returns a fetch config with user-simultated, generic headers and a proxy client object. Can be used as a fetch param.
	 *
	 * @param lastRefererUrl can accept the url of the last fetch call to simulate some kind of user redirection
	 * @param currentCookie can pass on set cookies from the website to its simulated that server set cookies will be passed again on subsequent requests.
	 * @return a config object for the deno webapi fetch function
	 */
	public getFetchConfig(lastRefererUrl?: string, currentCookie?: string): Object {
		let config: any = genericFetchConfig(this._userAgent!, lastRefererUrl);

		let basicAuth = undefined;
		if (this._user && this._password) {
			basicAuth = {
				username: this._user,
				password: this._password
			}
		}

		const client = Deno.createHttpClient({
			proxy: {
				url: this._address,
				basicAuth: basicAuth ? basicAuth : undefined,
			}
		})
		config.client = client;
		if (this._activeClient) this._activeClient.close();
		this._activeClient = client;
		if (currentCookie) { config.headers['cookie'] = currentCookie; }
		return config;
	}

	// deno-lint-ignore require-await
	public async block(): Promise<void> {
		throw new Error('GS: roxy isnt working, is captchaed, or blocked. We cannot fetch with this config');
	}

	public close(): void {
		this._activeClient?.close();
	}
}


export class GoogleScholarProxy extends Proxy implements IProxy {
	public onCooldown = false;
	public isCurrentlyUsed = false;
	public isWorking = true;
	private _cooldown = CONFIG.googleScholar.proxy.cooldown;


	/**
	 *	Set this proxy on a cooldown
	 *  Block this proxy instance for some time, so that it wont get banned or blocked by a webserver.
	 */
	// deno-lint-ignore require-await
	public async block(): Promise<void> {
		//this._cooldown = 200;
		this.onCooldown = true;
		this.isCurrentlyUsed = false;
		setTimeout(() => {
			this.onCooldown = false;
		}, this._cooldown)
	}

}

export class TorProxy extends Proxy implements IProxy {

	/**
	 * Restart the tor docker container to the daemon gets restartet and a new ip gets assigned if the current tor ip got captched or blocked
	 * Block this proxy instance for some time, so that it wont get banned or blocked by a webserver.
	 */
	public async block(): Promise<void> {
		logger.warning("GS: Tor Proxy got blocked. Reloading service to get new ip. Make sure the executing user is allowed to restart the tor docker container. See documentation for more info!");
		this._userAgent = HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)];

		// https://stackoverflow.com/questions/62142699/how-do-i-run-an-arbitrary-shell-command-from-deno
		const process = Deno.run({
			cmd: ["sudo", "docker", "restart", "snowballr_proxy"],
			stdout: "piped",
			stderr: "piped"
		});

		//const output = await process.output() // "piped" must be set
		//const outStr = new TextDecoder().decode(output);
		//console.log(outStr);

		const status: Deno.ProcessStatus = await process.status();
		if (status.success === false) {
			process.close();
			throw new Error("GS: Error handling tor service. Run deno as admin and check for tor being installed.");
		}
		let debugOutStr = "---"
		try {
			const debug = Deno.run({
				cmd: ["curl", "--socks5", "localhost:9050", "http://checkip.amazonaws.com/"],
				stdout: "piped",
				stderr: "piped"
			});
			const debugOut = await debug.output();
			debugOutStr = new TextDecoder().decode(debugOut);
			//console.log(debugOutStr);
			debug.close();
		}
		finally {
			logger.info(`GS: Successfully restarted Tor Docker Container. New IP is ${debugOutStr}`);
		}
		process.close();
	}
}

/**
 * Singleton used in GoogleScholar Api to manage various proxies and not overload them, so they wont get blocked.
 *
 * @param proxyPorts list of ports for the proxy. if not given is read from CONFIG yaml
 * @param proxyAddresses list of ips for the proxy. if not given is read from CONFIG yaml
 */
class ProxyManager {
	private static _instance: ProxyManager;
	private _proxies: Proxy[] = [] as Proxy[];
	private _settings: any;
	private _mode: "pool" | "tor";

	//allows to pass addresses and ports otherwise takes them from the env
	private constructor(proxyPorts?: number[], proxyAddresses?: number[]) {

		try {
			this._mode = CONFIG.googleScholar.proxy.mode;
		}
		catch (e) {
			throw new Error("GS: Cannot read config.yml from project source. Or config.yml isn't formatted correctly. Error: " + e.message);
		}
		if (this._mode === "pool") {
			this._settings = CONFIG.googleScholar;
			for (let p in CONFIG.googleScholar.proxy.urls) {
				let proxy = new GoogleScholarProxy(`${CONFIG.googleScholar.proxy.urls[p]}`)
				this._proxies.push(proxy);
			}
		}
		else if (this._mode === "tor") {
			this._proxies.push(new TorProxy(`${CONFIG.googleScholar.proxy.urls[0]}`));
		}
		else {
			throw new Error("GS: Invalid config.yaml config for googleScholar")
		}
	}

	// Make this object a singleton. The Deno way.
	public static Instance(proxyPorts?: number[], proxyAddresses?: number[]) {
		// Do you need arguments? Make it a regular static method instead.
		return this._instance || (this._instance = new this(proxyPorts ? proxyPorts : undefined, proxyAddresses ? proxyAddresses : undefined));
	}

	/**
	 * Try to get a free and working proxy config.
	 * @return a proxy object depending on the CONFIG settings
	 */
	public async acquire(): Promise<Proxy> {
		if (this._mode === "tor") {
			return this._proxies[0];
		}

		if (!this._settings.proxy.enabled) {
			logger.info(`GS: Proxy is disabled via config.yaml. Trying to run without a proxy`)
			throw new Error("GS: Tried to acquire a proxy. But proxy is disabled via config.")
		}
		console.debug("GS: Trying to acquire Proxy");
		let proxy: Proxy = {} as Proxy;
		this._proxies.forEach((p: any) => {
			if (!p.isCurrentlyUsed && !p.onCooldown) {
				p.isCurrentlyUsed = true;
				proxy = p;
			}
		});
		await sleep(1);
		return proxy;
	}

	/**
	 * Exchange a proxy that isnt working any more. and get a new one
	 * @param faultyProxy the proxy that is detected to not work anymore
	 * @return a proxy object depending on the CONFIG settings
	 */
	public exchange(faultyProxy: Proxy): Promise<Proxy> | undefined {
		logger.debug("Trying to EXCHANGE Proxy")
		faultyProxy.block();
		return this.acquire();
	}

}

// Just create a singleton since this is a management instance.
export const proxyPool = ProxyManager.Instance();

