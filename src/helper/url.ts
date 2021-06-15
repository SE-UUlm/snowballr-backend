export const urlSanitizer = (url: string) => {
    if (url.startsWith("http://")) {
        url = url.replace("http://", "https://");
    }
    if (!url.startsWith("https://")) {
        url = "https://" + url;
    }
    return url;
}