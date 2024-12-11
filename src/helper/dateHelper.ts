/**
 * Creates a numeric termination date for example a cookie.
 * It adds the days defined in the .env to the current time.
 */
export const createNumericTerminationDate = () => {
    const date = new Date(Date.now());
    const days = Number(Deno.env.get('DAYS_UNTIL_VALIDATION_GONE'));
    return date.setDate(date.getDate() + days);
}

export const createTokenExpiration = () => {
    return new Date(Date.now()+ 15*60*1000).getTime();
}