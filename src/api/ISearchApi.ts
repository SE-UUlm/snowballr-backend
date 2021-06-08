export interface ISearchApi {
    url: string;
    subscriptionKey: string;

    constructor(url: string, subscriptionKey: string): void;
    fetch(query: string): string;
}