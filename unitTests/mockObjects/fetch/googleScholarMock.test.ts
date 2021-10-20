export const headers = {
	"alt-svc": 'h3=":443"; ma=2592000,h3-29=":443"; ma=2592000,h3-Q050=":443"; ma=2592000,h3-Q046=":443"; ma=2592000...',
	"cache-control": "private, max-age=0",
	"content-type": "text/html; charset=UTF-8",
	date: "Wed, 20 Oct 2021 08:02:11 GMT",
	expires: "Wed, 20 Oct 2021 08:02:11 GMT",
	p3p: 'CP="This is not a P3P policy! See g.co/p3phelp for more info."',
	server: "scholar",
	"set-cookie": "GSP=LM=1634716931:S=F8wDH2CGyMndoBqK; expires=Fri, 20-Oct-2023 08:02:11 GMT; path=/; domain=scholar....",
	"transfer-encoding": "chunked",
	"x-content-type-options": "nosniff",
	"x-frame-options": "SAMEORIGIN",
	"x-xss-protection": "0"
};

export const paperResponse: string = Deno.readTextFileSync(`${new URL('.', import.meta.url).pathname}/googleScholarHtmlMock1.txt`);

export const citationResponse: string = Deno.readTextFileSync(`${new URL('.', import.meta.url).pathname}/googleScholarHtmlMock2.txt`);

export const parsedPaperNoCites = { "id": undefined, "title": ["Translation of UML 2 Activity Diagrams into finite state machines for model checking"], "author": [{ "orcid": [], "rawString": ["A Raschke"], "lastName": [], "firstName": [], "id": undefined, }], "abstract": [], "numberOfReferences": [], "numberOfCitations": [0], "year": [2009], "publisher": ["2009 35th Euromicro Conference on Software …"], "type": [], "scope": [], "scopeName": [], "pdf": [], "uniqueId": [], "source": ["googleScholar"], "raw": [] }

export const parsedPaper = { "id": undefined, "title": ["Translation of UML 2 Activity Diagrams into finite state machines for model checking"], "author": [{ "orcid": [], "rawString": ["A Raschke"], "lastName": [], "firstName": [], "id": undefined, }], "abstract": [], "numberOfReferences": [], "numberOfCitations": [8], "year": [2009], "publisher": ["2009 35th Euromicro Conference on Software …"], "type": [], "scope": [], "scopeName": [], "pdf": [], "uniqueId": [], "source": ["googleScholar"], "raw": [] };

export const parsedCitations = [
	{
		"id": undefined,
		"title": [
			"System behavior models: a survey of approaches"
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"SR Ruppel"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [
			8
		],
		"year": [
			2016
		],
		"publisher": [],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [
			"https://apps.dtic.mil/sti/pdfs/AD1026811.pdf"
		],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	},
	{
		"id": undefined,
		"title": [
			"Systematic derivation of state machines from communication-oriented business process models"
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"S España"
				],
				"lastName": [],
				"firstName": []
			},
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"M Ruiz"
				],
				"lastName": [],
				"firstName": []
			},
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"Ó Pastor…"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [
			7
		],
		"year": [
			2011
		],
		"publisher": [
			"2011 FIFTH …"
		],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [
			"https://www.academia.edu/download/47866550/Systematic_derivation_of_state_machines_20160807-27390-1uysx2e.pdf"
		],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	},
	{
		"id": undefined,
		"title": [
			"A formal semantics for sysml activity diagrams"
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"L Lima"
				],
				"lastName": [],
				"firstName": []
			},
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"A Didier"
				],
				"lastName": [],
				"firstName": []
			},
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"M Cornélio"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [
			6
		],
		"year": [
			2013
		],
		"publisher": [
			"Brazilian Symposium on Formal Methods"
		],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	},
	{
		"id": undefined,
		"title": [
			"From Formal Semantics to Executable Models: A Pragmatic Approach to Model-Driven Development."
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"H Partsch"
				],
				"lastName": [],
				"firstName": []
			},
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"M Dausend"
				],
				"lastName": [],
				"firstName": []
			},
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"D Gessenharter…"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [
			4
		],
		"year": [
			2011
		],
		"publisher": [
			"Int. J. Softw …"
		],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [
			"http://ijsi.cnjournals.com/ch/reader/create_pdf.aspx?file_no=i85&flag=1&journal_id=ijsi&year_id=2011"
		],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	},
	{
		"id": undefined,
		"title": [
			"Zur automatischen Verifikation von UML 2 Aktivitätsdiagrammen"
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"A Raschke"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [
			2
		],
		"year": [
			2010
		],
		"publisher": [],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [
			"https://oparu.uni-ulm.de/xmlui/bitstream/handle/123456789/1746/vts_7175_10094.pdf?sequence=1&isAllowed=y"
		],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	},
	{
		"id": undefined,
		"title": [
			"A Framework for Specifying and Analyzing Temporal Properties of UML Class Models."
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"M Al-Lail"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [
			1
		],
		"year": [
			2013
		],
		"publisher": [
			"Demos/Posters/StudentResearch@ MoDELS"
		],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [
			"https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.407.8433&rep=rep1&type=pdf"
		],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	},
	{
		"id": undefined,
		"title": [
			"Hybrid model checking approach to analysing rule conformance applied to HIPAA privacy rules, A"
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"P Bennett"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [],
		"year": [
			2017
		],
		"publisher": [],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [
			"https://mountainscholar.org/bitstream/handle/10217/183853/Bennett_colostate_0053A_14197.pdf?sequence=1"
		],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	},
	{
		"id": undefined,
		"title": [
			"A Unified Modeling Language Framework for Specifying and Analyzing Temporal Properties"
		],
		"author": [
			{
				"id": undefined,
				"orcid": [],
				"rawString": [
					"M Al Lail"
				],
				"lastName": [],
				"firstName": []
			}
		],
		"abstract": [],
		"numberOfReferences": [],
		"numberOfCitations": [],
		"year": [
			2018
		],
		"publisher": [],
		"type": [],
		"scope": [],
		"scopeName": [],
		"pdf": [
			"https://mountainscholar.org/bitstream/handle/10217/191492/AlLail_colostate_0053A_15099.pdf?sequence=1"
		],
		"uniqueId": [],
		"source": [
			"googleScholar"
		],
		"raw": []
	}
]