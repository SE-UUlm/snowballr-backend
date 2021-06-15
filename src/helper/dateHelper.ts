/**
 * Creates a numeric termination date for example a cookie.
 * It adds the days defined in the .env to the current time.
 */
export const createNumericTerminationDate = () => {
    let date = new Date(Date.now());
    let days = Number(Deno.env.get('DAYS_UNTIL_VALIDATION_GONE'));
    return date.setDate(date.getDate() + days);
}