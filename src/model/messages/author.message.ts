import { Status } from "./papersMessage.ts";

export interface AuthorMessage {
  id?: number;
  firstName?: string;
  lastName?: string;
  rawString?: string;
  orcid?: string;
  status?: Status;
}
