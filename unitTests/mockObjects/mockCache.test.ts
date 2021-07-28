import { SourceApi } from "../../src/api/iApiPaper.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { idType } from "../../src/api/iApiUniqueId.ts";

export const apiResponse: IApiResponse = {
	paper: {
		id: 1,
		title: ["Adaptive Exterior Light and Speed Control System"],
		author: [
			{
				id: 1,
				orcid: [],
				rawString: ["Frank, Houdek"],
				lastName: ["Houdek"],
				firstName: ["Frank"]
			},
			{
				id: 2,
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
			{ id: 1, type: idType.DOI, value: "0302-9743" },
			{ id: 2, type: idType.DOI, value: "10.1007/978-3-030-48077-6_24" }
		],
		source: [SourceApi.CR],
		raw: []
	},
	citations: [],
	references: [],
}