export const assign = (target: Object, source: Object) => {
    let t = <any>target
    let s = <any>source
    for (const key in source) {
        const val = s[key];
        if (val) {
            t[key] = val;
        }
    }
    return t;
}