import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { MicrosoftResearchApi } from "./microsoftResearchApi.ts";
import { OpenCitationsApi } from "./openCitationsApi.ts";
import { logger, fileLogger } from "./logger.ts";
import { CrossRefApi } from "./crossRefApi.ts";
import { ApiMerger } from "./apiMerger.ts";
import { SemanticScholar } from "./semanticScholar.ts"
import { IApiPaper } from "./iApiPaper.ts";
import { IeeeApi } from "./ieeeApi.ts";

const query: IApiQuery = {
	rawName: "sebastian erdweg",
	id: "10.1109/SEAA.2009.60",
	title: "The State of the Art in Language Workbenches"
}

const sortPapersByName = (item1: IApiPaper, item2: IApiPaper) => {
	if (item1.title && item2.title) {
		if (item1.title[0].toLowerCase() < item2.title[0].toLowerCase()) {
			return -1
		} else {
			return 1
		}
	}
	return 0
}

// const query: IApiQuery = {
//     rawName: "alexander raschke",
//     id: "10.1007/978-3-030-48077-6_24",
//     title: "Adaptive Exterior Light and Speed Control System"
// }

// const microsoft = new MicrosoftResearchApi("https://api.labs.cognitive.microsoft.com/academic/v1.0/evaluate", "9a02225751354cd29397eba3f5382101");
// const res = microsoft.fetch(query);
// const openCitations = new OpenCitationsApi("https://opencitations.net",)
// const res2 = openCitations.fetch(query);
// const crossRef = new CrossRefApi("https://api.crossref.org/works", "lukas.romer@uni-ulm.de");
// const res3 = crossRef.fetch(query);
// const semanticScholar = new SemanticScholar("https://api.semanticscholar.org/v1/paper");
// const res4 = semanticScholar.fetch(query);
const ieee = new IeeeApi("http://ieeexploreapi.ieee.org/api/v1/search/articles", "4yk5d9an52ejynjsmzqxe62r");
const res5 = ieee.fetch(query);

const merger = new ApiMerger();
//console.log((await res3).references);
//@ts-ignore
// (await res).citations.forEach((item) => {
//     if (item.title && item.title[0] && item.title[0].toLowerCase().includes("metar")) {
//         console.log("RESPONSE FROM MICRO: " + JSON.stringify(item, null, 2))
//     }
// })

// let first = JSON.parse(JSON.stringify((await res).citations));
// if (first) {
// 	first = first.sort(sortPapersByName)

// }

// let second = JSON.parse(JSON.stringify((await res3).citations));
// if (second) {
// 	second = second.sort(sortPapersByName)
// }

let doMerge = merger.compare([res5]).then(data => {
	//console.log(JSON.stringify(data, null, 2));
	for (let i = 0; i < data.length; i++) {

		fileLogger.info(`PAPER${i}:`);
		fileLogger.info(data[i].paper);
		fileLogger.info("CITATIONS:");

		let citeOriginal = data[i].citations;
		if (citeOriginal) {
			citeOriginal = citeOriginal.sort(sortPapersByName)
		}
		for (let cite in citeOriginal) {
			fileLogger.info((citeOriginal as any)[cite]);
		}


		let refOriginal = data[i].references;
		if (refOriginal) {
			refOriginal = refOriginal.sort(sortPapersByName)
		}
		fileLogger.info("REFERENCES:");
		for (let ref in refOriginal) {
			fileLogger.info((data[i].references as any)[ref]);
		}
	}

});

await doMerge;
// fileLogger.info("1 references")
// for (let cite in first) {
// 	fileLogger.info((first as any)[cite]);
// }
// fileLogger.info("2 references")
// for (let cite in second) {
// 	fileLogger.info((second as any)[cite]);
// }


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