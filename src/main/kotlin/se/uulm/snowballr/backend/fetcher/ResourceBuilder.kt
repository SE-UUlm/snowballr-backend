package se.uulm.snowballr.backend.fetcher

import java.util.ArrayList

/**
 * A utility class for building a list of resource file paths.
 *
 * @param prefix The prefix to be added to all file and directory paths.
 */
class ResourceBuilder(
    private val prefix: String,
) {
    private val files = ArrayList<String>()

    /**
     * Adds a file path to the list of resources.
     *
     * @param filename The name of the file to be added.
     */
    fun file(filename: String) {
        val newFile = "$prefix/$filename"
        if (files.contains(newFile)) {
            return
        }
        files.add(newFile)
    }

    /**
     * Descends into a directory to add files within that.
     *
     * @param dirname The name of the directory to be descended into.
     * @param block A lambda function to define the resources within the directory using the `file` and `dir` functions.
     */
    fun dir(dirname: String, block: ResourceBuilder.() -> Unit) {
        val builder = ResourceBuilder("$prefix/$dirname")
        builder.block()
        files.addAll(builder.getFiles())
    }

    /**
     * Retrieves the list of all resource file paths.
     *
     * @return A list of resource file paths.
     */
    fun getFiles(): List<String> = files
}

/**
 * Build a list of resources using the [ResourceBuilder].
 *
 * @param prefix The prefix to be added to all file and directory paths.
 * @param block A lambda function to define the resources within the directory using the `file` and `dir` functions.
 * @return A list of resource file paths.
 * @sample buildResourcesSample
 */
fun buildResources(prefix: String = "", block: ResourceBuilder.() -> Unit): List<String> {
    val builder = ResourceBuilder(prefix)
    builder.block()
    return builder.getFiles()
}

@Suppress("UnusedPrivateFunction")
private fun buildResourcesSample() {
    val filePaths = buildResources("/common/prefix") {
        file("foo.txt")
        dir("test") {
            file("bar.txt")
        }
    }

    assert(
        filePaths == listOf(
            "/common/prefix/foo.txt",
            "/common/prefix/test/bar.txt",
        ),
    )
}
