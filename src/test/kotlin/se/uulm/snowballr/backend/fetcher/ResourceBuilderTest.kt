package se.uulm.snowballr.backend.fetcher

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResourceBuilderTest {
    @Test
    fun `When no resources are added, then the builder should return an empty list`() = runTest {
        val resources = ResourceBuilder("/")
        assertThat(resources.getFiles()).isEmpty()
    }

    @Test
    fun `When a file is added, then it should be returned with the prefix`() = runTest {
        val resources = ResourceBuilder("foo")
        resources.file("bar")
        val files = resources.getFiles()
        assertEquals(1, files.size)
        assertEquals("foo/bar", files[0])
    }

    @Test
    fun `When the prefix is empty and a file is added, then it should be prefixed with a slash`() = runTest {
        val resources = ResourceBuilder("")
        resources.file("foo")
        val files = resources.getFiles()
        assertEquals(1, files.size)
        assertEquals("/foo", files[0])
    }

    @Test
    fun `When a directory is added, then no additional file should be returned`() = runTest {
        val resources = ResourceBuilder("")
        resources.dir("foo") {}
        val files = resources.getFiles()
        assertEquals(0, files.size)
    }

    @Test
    fun `When a directory is added and within that a file, then that file is returned relative to the topmost resource builder`() =
        runTest {
            val resources = ResourceBuilder("")
            resources.dir("foo") {
                file("bar")
            }
            val files = resources.getFiles()
            assertEquals(1, files.size)
            assertEquals("/foo/bar", files[0])
        }

    @Test
    fun `When a directory with slashes is added, then it should behave like multiple calls to dir`() = runTest {
        val resources = ResourceBuilder("")
        resources.dir("foo/bar") {
            file("a")
        }
        resources.dir("foo") {
            dir("bar") {
                file("b")
            }
        }
        val files = resources.getFiles()
        assertEquals(2, files.size)
        assertEquals("/foo/bar/a", files[0])
        assertEquals("/foo/bar/b", files[1])
    }

    @Test
    fun `When the same file is added twice, then it should only be returned once`() = runTest {
        val resources = ResourceBuilder("")
        resources.file("foo")
        resources.file("foo")
        val files = resources.getFiles()
        assertEquals(1, files.size)
        assertEquals("/foo", files[0])
    }

    @Test
    fun `When using the buildResources convenience function, then it should behave like the class`() = runTest {
        val builderFiles = buildResources("/foo") {
            file("a")
            dir("bar") {
                file("b")
            }
        }
        val classFiles = with(ResourceBuilder("/foo")) {
            file("a")
            dir("bar") {
                file("b")
            }
            getFiles()
        }

        assertThat(builderFiles).containsExactlyElementsOf(classFiles)
    }
}
