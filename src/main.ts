import { DataTypes, Database, Model } from 'https://deno.land/x/denodb/mod.ts';
import { sleep } from "https://deno.land/x/sleep/mod.ts";

const PostgresDB = Deno.env.get("POSTGRES_DB");
const PostgresUser = Deno.env.get("POSTGRES_USER");
const PostgresPassword = Deno.env.get("POSTGRES_PASSWORD");

// await sleep(60)

const db = new Database('postgres', {
  host: 'postgresdb',
  username: PostgresUser,
  password: PostgresPassword,
  database: PostgresDB,
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