export const assign = (target: Object, source: Object) => {
	let t = <any>target
	let s = <any>source
	for (const key in source) {
		const val = s[key];
		if (val) {
			if (typeof val === "object") {
				t[key] = assign({}, s[key])
			} else if (Array.isArray(val)) {
				t[key] = Array.from(s[key])
			} else {
				t[key] = val;
			}
		}
	}
	return t;
}



export const isEqual = (first: Object, second: Object) => {
	let firstProbs = Object.getOwnPropertyNames(first)
	let secondProbs = Object.getOwnPropertyNames(second)

	if (firstProbs.length != secondProbs.length) {
		return false;
	}

	for (let i of firstProbs) {
		if (typeof (first as any)[i] === "object") {
			let answer = isEqual((first as any)[i], (second as any)[i])
			if (!answer) {
				return false
			}
		} else if ((first as any)[i] !== (second as any)[i]) {
			return false;
		}
	}

	return true;
}

export const makePromise = async (arg0: any): Promise<typeof arg0> => {
	return arg0
}

export const concatWithoutDuplicates = (a: any[], b: any[]) => {
	return a.concat(b.filter((item) => a.indexOf(item) < 0))
}