export interface IProxy {
  block(): Promise<void>;
  getFetchConfig(): Object;
}
