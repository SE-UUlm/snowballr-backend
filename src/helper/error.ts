import { Context } from "https://deno.land/x/oak@v11.1.0/mod.ts";
import { IApiPaper } from "../api/iApiPaper.ts";
import { IApiResponse } from "../api/iApiResponse.ts";
import { logger } from "../api/logger.ts";
/**
 * Adds an error message to the oak context
 *
 * @param ctx
 * @param httpStatusCode
 * @param message
 */
export const makeErrorMessage = (ctx: Context, httpStatusCode: number, message?: string) => {
	ctx.response.status = httpStatusCode;
	if (message) {
		ctx.response.body = `{"error": "${message}"}`
	}
}

export const warnApiDisabledByConfig = (apiName: string): IApiResponse => {
	logger.warning(`${apiName} is disbaled via config.yaml. There wont be any results coming in!!!`)

	const apiReturn: IApiResponse = {
		"paper": {} as IApiPaper,
		"citations": [],
		"references": []
	}
	return apiReturn;
}