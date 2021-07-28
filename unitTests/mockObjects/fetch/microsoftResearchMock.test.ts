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
	"expr": "Or(DOI='10.1109/SEAA.2009.60', And(Composite(AA.AuN='alexander raschke'), Ti='translation of uml 2 activity diagrams into finite state machines for model checking'))",
	"entities": [
		{
			"logprob": -21.459,
			"prob": 4.791536e-10,
			"Id": 2124420229,
			"Ti": "translation of uml 2 activity diagrams into finite state machines for model checking",
			"Pt": "3",
			"Y": 2009,
			"CC": 8,
			"RId": [
				2115309705,
				1962072139,
				1489391022,
				1559012732,
				1560870874,
				2120450154,
				2066210260,
				2693416,
				2152178541,
				2123814724,
				1593514775,
				2170067116,
				2406837685,
				144098610
			],
			"DOI": "10.1109/SEAA.2009.60",
			"PB": "IEEE",
			"IA": {
				"IndexLength": 97,
				"InvertedIndex": {
					"Activity": [
						0
					],
					"diagrams": [
						1,
						63,
						88
					],
					"are": [
						2
					],
					"part": [
						3
					],
					"of": [
						4,
						75,
						86
					],
					"the": [
						5,
						42,
						83,
						94
					],
					"Unified": [
						6
					],
					"Modeling": [
						7
					],
					"Language": [
						8
					],
					"(UML)": [
						9
					],
					"to": [
						10,
						56,
						64,
						93
					],
					"specify": [
						11
					],
					"a": [
						12,
						36,
						73,
						78
					],
					"system’s": [
						13
					],
					"behavior.": [
						14
					],
					"This": [
						15,
						67
					],
					"formalism": [
						16
					],
					"has": [
						17
					],
					"been": [
						18
					],
					"substantially": [
						19
					],
					"revised": [
						20
					],
					"in": [
						21
					],
					"UML": [
						22
					],
					"2.": [
						23
					],
					"Concepts": [
						24
					],
					"like": [
						25
					],
					"signal": [
						26
					],
					"handling": [
						27
					],
					"and": [
						28
					],
					"interruptible": [
						29
					],
					"activity": [
						30,
						65,
						87
					],
					"regions": [
						31
					],
					"were": [
						32
					],
					"introduced.": [
						33
					],
					"By": [
						34
					],
					"using": [
						35
					],
					"token": [
						37
					],
					"flow": [
						38
					],
					"semantics": [
						39
					],
					"for": [
						40,
						61
					],
					"describing": [
						41
					],
					"execution,": [
						43
					],
					"activities": [
						44,
						76
					],
					"drift": [
						45
					],
					"apart": [
						46
					],
					"from": [
						47
					],
					"state": [
						48,
						62,
						79
					],
					"diagrams.": [
						49,
						66
					],
					"Therefore,": [
						50
					],
					"it": [
						51
					],
					"is": [
						52,
						69
					],
					"no": [
						53
					],
					"more": [
						54
					],
					"possible": [
						55
					],
					"apply": [
						57
					],
					"verification": [
						58
					],
					"techniques": [
						59
					],
					"designed": [
						60
					],
					"problem": [
						68
					],
					"faced": [
						70
					],
					"by": [
						71
					],
					"introducing": [
						72
					],
					"transformation": [
						74
					],
					"into": [
						77
					],
					"transition": [
						80
					],
					"system": [
						81
					],
					"covering": [
						82
					],
					"basic": [
						84
					],
					"concepts": [
						85
					],
					"including": [
						89
					],
					"but": [
						90
					],
					"not": [
						91
					],
					"limited": [
						92
					],
					"aforementioned": [
						95
					],
					"ones.": [
						96
					]
				}
			},
			"S": [
				{
					"U": "http://ieeexplore.ieee.org/document/5349867/"
				},
				{
					"Ty": 1,
					"U": "http://yadda.icm.edu.pl/yadda/element/bwmeta1.element.ieee-000005349867"
				},
				{
					"Ty": 1,
					"U": "https://dblp.uni-trier.de/db/conf/euromicro/euromicro2009.html#Raschke09"
				},
				{
					"Ty": 1,
					"U": "https://ieeexplore.ieee.org/document/5349867/"
				},
				{
					"Ty": 1,
					"U": "https://oparu.uni-ulm.de/xmlui/handle/123456789/25725"
				}
			],
			"AA": [
				{
					"AuN": "alexander raschke"
				}
			]
		}
	],
	"timed_out": false
} as any


export const parsedPaper = {
	"title": [
		"translation of uml 2 activity diagrams into finite state machines for model checking"
	],
	"author": [
		{
			"orcid": [],
			"rawString": [
				"alexander raschke"
			],
			"lastName": [],
			"firstName": []
		}
	],
	"abstract": [
		"Activity diagrams are part of the Unified Modeling Language (UML) to specify a system’s behavior. This formalism has been substantially revised in UML 2. Concepts like signal handling and interruptible activity regions were introduced. By using token flow semantics for describing execution, activities drift apart from state diagrams. Therefore, it is no more possible apply verification techniques designed problem faced by introducing transformation into transition system covering basic concepts including but not limited aforementioned ones."
	],
	"numberOfReferences": [
		14
	],
	"numberOfCitations": [
		8
	],
	"year": [
		2009
	],
	"publisher": [
		"IEEE"
	],
	"type": [
		"Conference paper"
	],
	"scope": [],
	"scopeName": [],
	"pdf": [
		"http://ieeexplore.ieee.org/document/5349867/",
		"http://yadda.icm.edu.pl/yadda/element/bwmeta1.element.ieee-000005349867",
		"https://dblp.uni-trier.de/db/conf/euromicro/euromicro2009.html#Raschke09",
		"https://ieeexplore.ieee.org/document/5349867/",
		"https://oparu.uni-ulm.de/xmlui/handle/123456789/25725"
	],
	"uniqueId": [
		{
			"type": "DOI",
			"value": "10.1109/SEAA.2009.60"
		},
		{
			"type": "MicrosoftAcademic",
			"value": 2124420229
		}
	],
	"source": [
		"microsoftAcademic"
	],
	"raw": []
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