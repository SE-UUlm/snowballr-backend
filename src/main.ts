import { DataTypes, Database, Model } from 'https://deno.land/x/denodb/mod.ts';
import { sleep } from "https://deno.land/x/sleep/mod.ts";
import { config } from "https://deno.land/x/dotenv/mod.ts";

var PostgresHost;
var PostgresDB;
var PostgresUser;
var PostgresPassword;

function getEnv() {
  PostgresHost = Deno.env.get("POSTGRES_HOST");
  PostgresDB = Deno.env.get("POSTGRES_DB");
  PostgresUser = Deno.env.get("POSTGRES_USER");
  PostgresPassword = Deno.env.get("POSTGRES_PASSWORD");
}
getEnv();

if (PostgresHost == undefined) {
  PostgresHost = "postgresdb"
}
if (PostgresDB == undefined || PostgresUser == undefined || PostgresPassword == undefined) {
  console.log("Hello");
  config({export: true});
  console.log(Deno.env.get("POSTGRES_DB"));
  getEnv();
}

const db = new Database('postgres', {
  host: PostgresHost,
  username: PostgresUser,
  password: PostgresPassword,
  database: PostgresDB
});


class Flight extends Model {
  static table = 'flights';
  static timestamps = true;

  static fields = {
    id: { primaryKey: true, autoIncrement: true },
    departure: DataTypes.STRING,
    destination: DataTypes.STRING,
    flightDuration: DataTypes.FLOAT,
  };

  static defaults = {
    flightDuration: 2.5,
  };
}

db.link([Flight]);

await db.sync({ drop: false });

await Flight.create({
  departure: 'Meersburg',
  destination: 'Tokyo',
});

// or

const flight = new Flight();
flight.departure = 'Ravensburg';
flight.destination = 'San Francisco';
await flight.save();

await console.log(Flight.select('destination').all());
// [ { destination: "Tokyo" }, { destination: "San Francisco" } ]


// await Flight.where('destination', 'Tokyo').delete();

const sfFlight = await Flight.find(2);
// { destination: "San Francisco" }

console.log(sfFlight);

await Flight.count();
// 1

await Flight.select('id', 'destination').orderBy('id').get();
// [ { id: "2", destination: "San Francisco" } ]

// await sfFlight.delete();

await db.close();