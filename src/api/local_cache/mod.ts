/*
MIT License

Copyright (c) 2020 SomberTM Luchsor

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/


import EventEmitter from "https://deno.land/std/node/events.ts";
export type IndexSignatureKey = string | number;
export type Map<V> = { [key: string]: V, [key: number]: V };
export type MapEntry<K, V> = [key: K, value: V];

declare interface CacheEvents<K extends IndexSignatureKey, V> {
	'ttl': (key: K, value: V, ttl: number) => void,
	'add': (key: K, value: V, ttl?: number) => void,
	'set': (key: K, value: V, ttl?: number) => void,
	'delete': (key: K, value: V) => void,
	'clear': (map: Map<V>) => void
}

export declare interface Cache<K extends IndexSignatureKey, V> {
	on<E extends keyof CacheEvents<K, V>>(
		event: E, listener: CacheEvents<K, V>[E]
	): this;

	emit<E extends keyof CacheEvents<K, V>>(
		event: E, ...args: Parameters<CacheEvents<K, V>[E]>
	): boolean;
}

export class Cache<K extends IndexSignatureKey, V> extends EventEmitter {

	public size: number = 0;
	public ttl: number;
	private map: Map<V> = {};
	private timeouts: Number[] = [];

	constructor(ttl: number = -1) {
		super();
		this.ttl = ttl;
	}

	public static isCache(value: any): boolean { return value.constructor.name == 'Cache'; }
	public static valueOf<K extends IndexSignatureKey, V>(map: Map<V>, ttl?: number): Cache<K, V>;
	public static valueOf<K extends IndexSignatureKey, V>(entries: MapEntry<K, V>[], ttl?: number): Cache<K, V>;
	public static valueOf<K extends IndexSignatureKey, V>(cache: Cache<K, V>, ttl?: number): Cache<K, V>;
	public static valueOf<K extends IndexSignatureKey, V>(value: Map<V> | MapEntry<K, V>[] | Cache<K, V>, ttl?: number): Cache<K, V> {
		const cache: Cache<K, V> = new Cache<K, V>(Cache.isCache(value) ? (<Cache<K, V>>value).ttl : ttl);
		if (Array.isArray(value)) {
			value.forEach(([key, value]: MapEntry<K, V>) => cache.add(key, value));
		} else if (Cache.isCache(value)) {
			(<Cache<K, V>>value).entries().forEach(([key, value]: MapEntry<K, V>) => cache.add(key, value));
		} else {
			(<MapEntry<K, V>[]>Object.entries(value)).forEach(([key, value]: MapEntry<K, V>) => cache.add(key, value));
		}
		return cache;
	};

	private deleteAfterTTL(key: K, ttl: number = -1) {
		const primaryttl: number = ttl > 0 ? ttl : this.ttl > 0 ? this.ttl : -1;
		if (primaryttl > 0) {
			const timeout = setTimeout(() => {
				if (this.has(key)) {
					const value: V = this.map[key];
					delete this.map[key];
					this.timeouts.splice(this.timeouts.indexOf(timeout), 1)
					this.emit('ttl', key, value, primaryttl);
				}
			}, primaryttl);
			this.timeouts.push(timeout);
		}
	}

	public get(key: K): V { return this.map[key]; }
	public add(key: K, value: V, ttl?: number) {
		if (!this.has(key)) {
			this.map[key] = value;
			this.size++;
			this.deleteAfterTTL(key, ttl);
			this.emit('add', key, value, ttl || this.ttl);
			return true;
		}; return false;
	}
	public set(key: K, value: V, ttl?: number) { if (!this.add(key, value, ttl)) { this.map[key] = value; this.emit('set', key, value, ttl || this.ttl); } }
	public has(key: K): boolean { return this.map.hasOwnProperty(key); }
	public clear(): void {
		this.emit('clear', this.map); this.map = {}; this.timeouts.forEach((v, i) => {
			clearTimeout(<number>v); this.timeouts.splice(i, 1)
		})
	}
	public delete(key: K) { this.emit('delete', key, this.map[key]); delete this.map[key]; }

	public first(): V | undefined { if (!this.empty()) return this.values()[0]; }
	public firstKey(): K | undefined { if (!this.empty()) return this.keys()[0]; }
	public last(): V | undefined { if (!this.empty()) return this.values()[this.size - 1]; }
	public lastKey(): K | undefined { if (!this.empty()) return this.keys()[this.size - 1]; }
	public empty(): boolean { return this.size > 0 ? false : true; }
	public clone(): Cache<K, V> { return Cache.valueOf(this.map, this.ttl); }

	public randomValue(): V {
		let index: number = Math.floor(Math.random() * this.size);
		return this.values()[index];
	}

	public randomKey(): K {
		let index: number = Math.floor(Math.random() * this.size);
		return this.keys()[index];
	}

	public keys(): K[] { return <K[]>Object.keys(this.map); }
	public values(): V[] { return Object.values(this.map); }
	public entries(): MapEntry<K, V>[] { return <MapEntry<K, V>[]>Object.entries(this.map); }

	public forEach(callbackfn: (value: V, index?: number, array?: V[]) => void, thisArg?: any) {
		this.values().forEach(callbackfn, thisArg);
	}

	public forEachKey(callbackfn: (value: K, index?: number, array?: K[]) => void, thisArg?: any) {
		this.keys().forEach(callbackfn, thisArg);
	}

	public forEachEntry(callbackfn: (value: MapEntry<K, V>, index?: number, array?: MapEntry<K, V>[]) => void, thisArg?: any) {
		this.entries().forEach(callbackfn, thisArg);
	}

	public find(predicate: (value: V, index?: number, obj?: V[]) => boolean, thisArg?: any): V | undefined {
		return this.values().find(predicate, thisArg);
	}

	public findKey(predicate: (value: K, index?: number, obj?: K[]) => boolean, thisArg?: any): K | undefined {
		return this.keys().find(predicate, thisArg);
	}

	public findEntry(predicate: (value: MapEntry<K, V>, index?: number, obj?: MapEntry<K, V>[]) => boolean, thisArg?: any): MapEntry<K, V> | undefined {
		return this.entries().find(predicate, thisArg);
	}

	public toString = (): string => {
		return `${this.constructor.name} ${JSON.stringify(this.map, null, 2)}`;
	}

}