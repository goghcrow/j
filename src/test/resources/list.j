assert [1, ] == [1]

// π θ΄ζ°ζ°η»δΈζ 
{
    val arr = [1,2,3,]
    assert arr[-1] == 3
    assert arr[-2] == 2
    assert arr[-3] == 1
}

assert [1,2] + [3,4] == [1, 2, 3, 4]

{
    val lst = []
    lst << 1
    lst << 2
    assert lst == [1, 2]
}