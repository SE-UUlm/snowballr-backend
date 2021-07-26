export const headers = new Headers({
	"access-control-allow-headers": "X-Requested-With",
	"access-control-allow-origin": "*",
	connection: "close",
	"content-type": "application/json",
	date: "Wed, 21 Jul 2021 08:56:32 GMT",
	"permissions-policy": "interest-cohort=()",
	server: "Jetty(9.4.40.v20210413)",
	vary: "Accept-Encoding",
	"x-rate-limit-interval": "1s",
	"x-rate-limit-limit": "50",
	"x-ratelimit-interval": "1s",
	"x-ratelimit-limit": "50"
})

export const paperResponse = {
	'message': {
		'publisher': "Springer International Publishing",
		DOI: "10.1007/978-3-030-48077-6_24",
		type: "book-chapter",
		title: ["Adaptive Exterior Light and Speed Control System"],
		author: [
			{ given: "Frank", family: "Houdek", sequence: "first", affiliation: [Array] },
			{
				given: "Alexander",
				family: "Raschke",
				sequence: "additional",
				affiliation: [Array]
			}
		],
		ISSN: ["0302-9743", "1611-3349"],
		"references-count": 4,
		link: [
			{
				URL: "http://link.springer.com/content/pdf/10.1007/978-3-030-48077-6_24",
				"content-type": "unspecified",
				"content-version": "vor",
				"intended-application": "similarity-checking"
			}
		],
	}
} as any

export const parsedPaper = {
	id: undefined,
	title: ["Adaptive Exterior Light and Speed Control System"],
	author: [
		{
			id: undefined,
			orcid: [],
			rawString: ["Frank, Houdek"],
			lastName: ["Houdek"],
			firstName: ["Frank"]
		},
		{
			id: undefined,
			orcid: [],
			rawString: ["Alexander, Raschke"],
			lastName: ["Raschke"],
			firstName: ["Alexander"]
		}
	],
	abstract: [],
	numberOfReferences: [4],
	numberOfCitations: [],
	year: [],
	publisher: ["Springer International Publishing"],
	type: ["book-chapter"],
	scope: [],
	scopeName: [],
	pdf: ["http://link.springer.com/content/pdf/10.1007/978-3-030-48077-6_24"],
	uniqueId: [
		{ id: undefined, type: "ISSN", value: ["0302-9743", "1611-3349"] },
		{ id: undefined, type: "DOI", value: "10.1007/978-3-030-48077-6_24" }
	],
	source: ["crossRef"],
	raw: []
}


export const referenceRequest = {
	key: "24_CR1",
	"series-title": "Communications in Computer and Information Science",
	"doi-asserted-by": "publisher",
	"first-page": "1",
	DOI: "10.1007/978-3-319-07512-9_1",
	"volume-title": "ABZ 2014: The Landing Gear Case Study",
	author: "F Boniol",
	year: "2014",
	unstructured: "Boniol, F., Wiels, V.: The landing gear system case study. In: Boniol, F., Wiels, V., Ait Ameur, Y.,..."
}


export const referenceResponse = {
	status: "ok",
	"message-type": "work",
	"message-version": "1.0.0",
	message: {
		indexed: {
			"date-parts": [[Array]],
			"date-time": "2021-05-12T05:36:43Z",
			timestamp: 1620797803944
		},
		"publisher-location": "Cham",
		"reference-count": 0,
		publisher: "Springer International Publishing",
		"content-domain": { domain: [], "crossmark-restriction": false },
		"short-container-title": [],
		"published-print": { "date-parts": [[Array]] },
		DOI: "10.1007/978-3-319-07512-9_1",
		type: "book-chapter",
		created: {
			"date-parts": [[Array]],
			"date-time": "2014-05-12T02:23:18Z",
			timestamp: 1399861398000
		},
		page: "1-18",
		source: "Crossref",
		"is-referenced-by-count": 33,
		title: ["The Landing Gear System Case Study"],
		prefix: "10.1007",
		author: [
			{ given: "Frédéric", family: "Boniol", sequence: "first", affiliation: [Array] },
			{
				given: "Virginie",
				family: "Wiels",
				sequence: "additional",
				affiliation: [Array]
			}
		],
		member: "297",
		"container-title": [
			"Communications in Computer and Information Science",
			"ABZ 2014: The Landing Gear Case Study"
		],
		"original-title": [],
		link: [
			{
				URL: "http://link.springer.com/content/pdf/10.1007/978-3-319-07512-9_1",
				"content-type": "unspecified",
				"content-version": "vor",
				"intended-application": "similarity-checking"
			}
		],
		deposited: {
			"date-parts": [[Array]],
			"date-time": "2014-05-12T02:23:21Z",
			timestamp: 1399861401000
		},
		score: 1,
		subtitle: [],
		"short-title": [],
		issued: { "date-parts": [[Array]] },
		"references-count": 0,
		URL: "http://dx.doi.org/10.1007/978-3-319-07512-9_1",
		relation: {},
		ISSN: ["1865-0929", "1865-0937"],
		"issn-type": [
			{ value: "1865-0929", type: "print" },
			{ value: "1865-0937", type: "electronic" }
		],
		published: { "date-parts": [[Array]] }
	}
}

export const parsedReference = {
	id: undefined,
	title: ["The Landing Gear System Case Study"],
	author: [
		{
			id: undefined,
			orcid: [],
			rawString: ["Frédéric, Boniol"],
			lastName: ["Boniol"],
			firstName: ["Frédéric"]
		},
		{
			id: undefined,
			orcid: [],
			rawString: ["Virginie, Wiels"],
			lastName: ["Wiels"],
			firstName: ["Virginie"]
		}
	],
	abstract: [],
	numberOfReferences: [],
	numberOfCitations: [33],
	year: [],
	publisher: ["Springer International Publishing"],
	type: ["book-chapter"],
	scope: [],
	scopeName: [],
	pdf: ["http://link.springer.com/content/pdf/10.1007/978-3-319-07512-9_1"],
	uniqueId: [
		{ id: undefined, type: "ISSN", value: ["1865-0929", "1865-0937"] },
		{ id: undefined, type: "DOI", value: "10.1007/978-3-319-07512-9_1" }
	],
	source: ["crossRef"],
	raw: []
}