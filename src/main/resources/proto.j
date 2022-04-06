object prototype {
    fun copy(objVal) = Java.unbox(__.valueCopy(__.javaNull, Java.box(objVal)))
}

// export
prototype