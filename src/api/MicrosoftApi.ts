import { ISearchApi } from './ISearchApi.ts';

export class MicrosoftApi extends ISearchApi {
    url: string;
    subscriptionKey: string;

    constructor(url: string, subscriptionKey: string) {
        super();
        this.url = url;
        this.subscriptionKey = subscriptionKey;
    }

    fetch(query: string): string {
        let response = await fetch('https://api.labs.cognitive.microsoft.com/academic/v1.0/interpret?query=' + query + '&subscription-key=' + this.subscriptionKey);
        let data = await response.json();
        return String(data);
    }
}