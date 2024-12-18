import * as log from "https://deno.land/std@0.150.0/log/mod.ts";
import { toIMF } from "https://deno.land/std@0.150.0/datetime/mod.ts";

await log.setup({
  handlers: {
    console: new log.handlers.ConsoleHandler("DEBUG", {
      formatter: (logRecord) => {
        let msg = `${
          toIMF(new Date())
        }\t${logRecord.levelName}\t${logRecord.msg}`;

        logRecord.args.forEach((arg, index) => {
          msg += `, arg${index}: ${arg}`;
        });

        return msg;
      },
    }),
    file: new log.handlers.RotatingFileHandler("INFO", {
      filename: `${new URL(".", import.meta.url).pathname}/a.log`,
      maxBytes: 65000000,
      maxBackupCount: 5,
      mode: "w",
      formatter: (rec) => rec.msg,
    }),
  },

  //assign handlers to loggers
  loggers: {
    default: {
      level: "DEBUG",
      handlers: ["console"],
    },
    client: {
      level: "INFO",
      handlers: ["file"],
    },
  },
});
export const logger = log.getLogger();
export const fileLogger = log.getLogger("client");
