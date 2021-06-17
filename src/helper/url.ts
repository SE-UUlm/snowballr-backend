/**
 * Sanitizes an url to satisfy the standard needed to be able to click on it after getting it delivered by mail.
 * Also makes sure the link is set to start with https.
 * @param url
 */
export const urlSanitizer = (url: string) => {
    let port = Number(Deno.env.get("PORT"));
    if (port) {
        if (port !== 443) {
            url += port;
        }
    }
    if (url.startsWith("http://")) {
        url = url.replace("http://", "https://");
    }
    if (!url.startsWith("https://")) {
        url = "https://" + url;
    }
    return url;
}