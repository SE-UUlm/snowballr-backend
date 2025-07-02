package se.uulm.snowballr.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Executes a suspending function within a coroutine that runs on the main dispatcher.
 *
 * @param testFunction The suspending function to be executed during the test.
 */
fun testCoroutine(testFunction: suspend () -> Unit) {
    runBlocking {
        launch(Dispatchers.Main) {
            testFunction()
        }
    }
}
