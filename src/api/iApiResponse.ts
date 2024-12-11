import { IApiPaper } from "./iApiPaper.ts";

/**
 * Contains OCI mapped paper object of a single paper fetch for each the original paper, citations papers and reference papers
 */
export interface IApiResponse {
  paper: IApiPaper;
  references?: Array<IApiPaper>;
  citations?: Array<IApiPaper>;
}
