import { UserProfile } from "../userProfile.ts";

export interface LoginMessage {
  token: string;
  user: UserProfile;
}
