object internal {
    fun args_merge_defaults(args, defaults) {
        assert args is List
        if (defaults is List) {
            val argSz = args.size()
            val defSz = defaults.size()
            if (argSz < defSz) {
                for(var i = argSz; i < defSz; i++) {
                    args.add(defaults[i])
                }
            }
        }
        args
    }

    fun destruct(it) = it != null && it[`destruct`] is Fun ? it[`destruct`]() : it
}



// export
internal