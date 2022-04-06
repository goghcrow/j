fun println() {
    if (arguments.size() == 0) {
        __.println_stdOut("")
    } else {
        for (val a in arguments) {
            __.println_stdOut(a)
        }
    }
}

// export
global.println = println
global.require = sys.require
global.eval = sys.doString

null