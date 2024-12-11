/**
 * This interface is only provided so a test can be run without using a real email connection.
 */
export interface EMailClient {
  send({}): any;

  connect({}): any;

  close(): any;
}
