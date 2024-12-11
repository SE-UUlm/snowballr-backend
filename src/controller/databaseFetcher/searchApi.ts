import { ProjectUsesApi } from "../../model/db/projectUsesApi.ts";
import { SearchApi } from "../../model/db/searchApi.ts";

export const getAllApisFromProject = async (
  id: number,
): Promise<SearchApi[]> => {
  const projectUsesApi = await ProjectUsesApi.where(
    ProjectUsesApi.field("project_id"),
    id,
  ).join(SearchApi, SearchApi.field("id"), ProjectUsesApi.field("searchapi_id"))
    .get();

  if (Array.isArray(projectUsesApi)) {
    return projectUsesApi;
  }
  return new Array<SearchApi>();
};
