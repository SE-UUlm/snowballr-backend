import { Thread } from "https://deno.land/x/Thread@v3.0.0/Thread.ts";
import { ApiBatcher } from "./apiBatcher.ts";


export interface IThread {
	threadInstance: Thread;
	batcherInstances: ApiBatcher[];

}


const thread: Thread = new Thread<number>((e: MessageEvent) => {
	console.log('Worker: Message received from main script');
	const result = e.data[0] * e.data[1];
	if (isNaN(result)) {
		return 0;
	} else {
		console.log('Worker: Posting message back to main script');
		return (result);
	}
}, "module");