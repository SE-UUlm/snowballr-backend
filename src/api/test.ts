import {IApiQuery} from "./iApiQuery.ts";
import {IApiResponse} from "./iApiResponse.ts";
import {IApiFetcher} from "./iApiFetcher.ts";
import {MicrosoftResearchApi} from "./microsoftResearchApi.ts";
import {OpenCitationsApi} from "./openCitationsApi.ts";
import {logger, fileLogger} from "./logger.ts";
import {CrossRefApi} from "./crossRefApi.ts";
import {ApiMerger} from "./apiMerger.ts";
import {SemanticScholar} from "./semanticScholar.ts"

const query: IApiQuery = {
    rawName: "sebastian erdweg",
    id: "10.1007/978-3-319-02654-1_11",
    title: "The State of the Art in Language Workbenches"
}

// const query: IApiQuery = {
//     rawName: "alexander raschke",
//     id: "10.1007/978-3-030-48077-6_24",
//     title: "Adaptive Exterior Light and Speed Control System"
// }

const microsoft = new MicrosoftResearchApi("https://api.labs.cognitive.microsoft.com/academic/v1.0/evaluate", "9a02225751354cd29397eba3f5382101");
const res = microsoft.fetch(query);
// const openCitations = new OpenCitationsApi("https://opencitations.net",)
// const res2 = openCitations.fetch(query);
// const crossRef = new CrossRefApi("https://api.crossref.org/works");
// const res3 = crossRef.fetch(query);
const semanticScholar = new SemanticScholar("https://api.semanticscholar.org/v1/paper");
const res4 = semanticScholar.fetch(query);

const merger = new ApiMerger();
//console.log((await res3).references);
//@ts-ignore
// (await res).citations.forEach((item) => {
//     if (item.title && item.title[0] && item.title[0].toLowerCase().includes("metar")) {
//         console.log("RESPONSE FROM MICRO: " + JSON.stringify(item, null, 2))
//     }
// })
merger.compare([res, res4]).then(data => {
    //console.log(JSON.stringify(data, null, 2));
    for (let i = 0; i < data.length; i++) {

        fileLogger.info(`PAPER${i}:`);
        fileLogger.info(data[i].paper);
        fileLogger.info("CITATIONS:");
        for (let cite in data[i].citations) {
            fileLogger.info((data[i].citations as any)[cite]);
        }
        fileLogger.info("REFERENCES:");
        for (let ref in data[i].references) {
            fileLogger.info((data[i].references as any)[ref]);
        }
    }

});

// const crossRef = new CrossRefApi("https://api.crossref.org/works")
// const res3 = crossRef.fetch(query)
//     .then(data => console.log(JSON.stringify(data, null, 2)));

/*
TODO:
api crossref mit email


- heuristik bauen und per hand optimieren
- levinsteindistanz für strings im verhältnis zur länge des strings
- 20% der länge ( muss evtl zuwischen  db felder koheriert werden)

- super ähnliche titel vergleichen

- dois nicht zwingend unique. aggresive

- select one of similar papers via frontend

- snowball tinder

- journal papaer haben mehr referenzen und zitate als conferences

- crawler für references
*/