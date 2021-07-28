export const checkIApiAuthor = (author: { [index: string]: any }): boolean => {
    for (let i in author) {
        if (!["id"].includes(i)) {

            if (author[i] && author[i].length > 1) {
                return false;
            }
        }
    }
    return true;
}