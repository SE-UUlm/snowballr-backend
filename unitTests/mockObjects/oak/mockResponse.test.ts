/**
 * Mocks a response.
 * To lower functionality, the body if the response isn't defined as an object.
 * To verify the sent body of the application use JSON.parse(response.body as string).
 * The status is set to 999 since oak sets it standardized to 80, so an 80 can come back without being set by the backend.
 * @param headersInit
 */
export function createMockResponse(headersInit: string[][]): Response {
  const response = {
    headers: new Headers(headersInit),
    status: 999,
    body: undefined,
    redirect(url: string | URL) {
      this.headers.append("Location", encodeURI(String(url)));
    },
  };
  return response as any;
}
