object io {
    fun exist(f) = __.toVBool(__.fileExist(__.path(f)))

    fun read(f) = exist(f) ? __.toVStr(__.fileRead(__.path(f))) : null

    // io.write("/tmp/test", "...", ["APPEND"])   opts: java.nio.file.StandardOpenOption
    fun write(f, VStr, opts/*:List*/) = __.fileWrite(__.path(f), VStr, opts)

    fun delete(f) = __.toVBool(__.fileDelete(__.path(f)))

    fun mkdir(dir) = __.fileCreateDir(__.path(dir))

    fun isDir(p) = null
    fun size(p) = null
    fun walk(dir) = null
    fun find() = null
    fun move() = null
    // fun canRead() = null
    // fun canWrite() = null

}

io