import * as log from "https://deno.land/std/log/mod.ts";
import { toIMF } from "https://deno.land/std/datetime/mod.ts";

await log.setup({
    handlers: {
        console: new log.handlers.ConsoleHandler("INFO", {
            formatter: logRecord => {
                let msg = `${toIMF(new Date())}\t${logRecord.levelName}\t${logRecord.msg}`;

                logRecord.args.forEach((arg, index) => {
                    msg += `, arg${index}: ${arg}`;
                });

                return msg;
            }
        }),
        file: new log.handlers.RotatingFileHandler('INFO', {
            filename: './a.log',
            maxBytes: 15,
            maxBackupCount: 5,
            formatter: rec => JSON.stringify({
                region: rec.loggerName,
                ts: rec.datetime,
                level: rec.levelName,
                data: rec.msg
            })
        })
    },

    //assign handlers to loggers
    loggers: {
        default: {
            level: "DEBUG",
            handlers: ["console"],
        },
        client: {
            level: "INFO",
            handlers: ["file"]
        }
    },
});
export const logger = log.getLogger();
export const fileLogger = log.getLogger('client');