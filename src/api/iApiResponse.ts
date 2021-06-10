import {IApiPaper} from './iApiPaper.ts'

export interface IApiResponse {
    paper: IApiPaper;
    references?: Array<IApiPaper>;
    citations?: Array<IApiPaper>;
}