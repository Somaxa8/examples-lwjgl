/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package app.somacode.lwjgltest
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.awt.SystemColor.window


class App {
    var window: Long? = null
    var width: Int = 800
    var height: Int = 600
    var title: String = "My Game"

    var vertexShaderSrc = """
        #version 330 core
        layout (location=0) in vec3 aPos;
        layout (location=1) in vec4 aColor;

        out vec4 fColor;

        void main() {
            fColor = aColor;
            gl_Position = vec4(aPos, 1.0);
        }
    """.trimIndent()

    var fragmentShaderSrc = """
        #version 330 core

        in vec4 fColor;
        out vec4 color;

        void main() {
            color = fColor;
        }
    """.trimIndent()

    var vertexId: Int? = null
    var fragmentId: Int? = null
    var shaderProgram: Int? = null

    val vertexArray = floatArrayOf(
            // position         // color
            0.5f, -0.5f, 0.0f,  1.0f, 0.0f, 0.0f, 1.0f, // Bottom right 0
            -0.5f, 0.5f, 0.0f,  0.0f, 1.0f, 0.0f, 1.0f, // Top left     1
            0.5f, 0.5f, 0.0f,   1.0f, 0.0f, 1.0f, 1.0f, // Top right    2
            -0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f  // Bottom left  3
    )

    val elementArray = intArrayOf(
            /*
                    x1        x2
                    x3        x0
             */
            2, 1, 0,  // Top right triangle
            0, 1, 3 // bottom left triangle
    )

    var vaoId: Int = 0
    var vboId: Int = 0
    var eboId: Int = 0

    fun run() {
        println("Test LWJGL ${Version.getVersion()}")

        init()
        loop()

        glfwFreeCallbacks(window!!)
        glfwDestroyWindow(window!!)

        glfwTerminate();
        glfwSetErrorCallback(null)!!.free()
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Create the window
        window = glfwCreateWindow(width, height, title, NULL, NULL)
        if (window == NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window!!) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop
        }

        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window!!, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            // Center the window
            glfwSetWindowPos(
                    window!!,
                    (vidmode!!.width() - pWidth[0]) / 2,
                    (vidmode.height() - pHeight[0]) / 2
            )
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window!!)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window!!)
    }

    private fun getShaders() {
        // Compile vertex shader
        vertexId = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexId!!, vertexShaderSrc)
        glCompileShader(vertexId!!)

        var success = glGetShaderi(vertexId!!, GL_COMPILE_STATUS)

        if (success == GL_FALSE) {
            val len = glGetShaderi(vertexId!!, GL_INFO_LOG_LENGTH)
            println("Vertex shader compilation failed")
            println(glGetShaderInfoLog(vertexId!!, len))
            assert(false)
        }

        // Compile fragment shader
        fragmentId = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentId!!, fragmentShaderSrc)
        glCompileShader(fragmentId!!)

        success = glGetShaderi(fragmentId!!, GL_COMPILE_STATUS)

        if (success == GL_FALSE) {
            val len = glGetShaderi(fragmentId!!, GL_INFO_LOG_LENGTH)
            println("Fragment shader compilation failed")
            println(glGetShaderInfoLog(fragmentId!!, len))
            assert(false)
        }

        shaderProgram = glCreateProgram()
        glAttachShader(shaderProgram!!, vertexId!!)
        glAttachShader(shaderProgram!!, fragmentId!!)
        glLinkProgram(shaderProgram!!)

        success = glGetProgrami(shaderProgram!!, GL_LINK_STATUS)
        if (success == GL_FALSE) {
            val len = glGetProgrami(shaderProgram!!, GL_INFO_LOG_LENGTH)
            println("Linking of shaders failed")
            println(glGetProgramInfoLog(shaderProgram!!, len))
            assert(false)
        }

        // Generate VAO, VBO, and EBO buffer objects, and send to GPU

        // Create VAO (Vertex Array Object)
        vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)

        // Create float buffer of vertices
        val vertexBuffer = BufferUtils.createFloatBuffer(vertexArray.size)
        vertexBuffer.put(vertexArray).flip()

        // Create VBO (Vertex Buffer Object) upload the vertex buffer
        vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW)

        // Create the indices and upload
        val elementBuffer = BufferUtils.createIntBuffer(elementArray.size)
        elementBuffer.put(elementArray).flip()

        // Create EBO (Element Buffer Object)
        eboId = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW)

        // Add the vertex attribute pointers
        val positionsSize = 3
        val colorSize = 4
        val floatSizeBytes = 4
        val vertexSizeBytes = (positionsSize + colorSize) * floatSizeBytes

        glVertexAttribPointer(0, positionsSize, GL_FLOAT, false, vertexSizeBytes, 0)
        glEnableVertexAttribArray(0)

        glVertexAttribPointer(1, colorSize, GL_FLOAT, false, vertexSizeBytes, (positionsSize * floatSizeBytes).toLong())
        glEnableVertexAttribArray(1)
    }

    private fun draw() {
        // Bind shader program
        glUseProgram(shaderProgram!!)
        // Bind the VAO that we're using
        glBindVertexArray(vaoId)

        // Enable the vertex attribute pointers
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)

        glDrawElements(GL_TRIANGLES, elementArray.size, GL_UNSIGNED_INT, 0)

        // Unbind everything
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)

        glBindVertexArray(0)

        glUseProgram(0)
    }

    private fun loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        getShaders()

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window!!)) {
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
            // Set the clear color
            glClearColor(0f, 0f, 0f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer

            draw()

            glfwSwapBuffers(window!!) // swap the color buffers
        }
    }
}

fun main() {
    App().run()
}
