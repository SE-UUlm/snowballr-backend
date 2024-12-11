/**
 * This class is only for use, if no email should be sent
 */
export class MockEmailClient {
  send = ({}) => {
    return undefined;
  };
  connect = ({}) => {
    return undefined;
  };
  close = () => {
    return undefined;
  };
}
