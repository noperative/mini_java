class Debug {
    public static void main(String[] a) {
        System.out.println(new Test(). go ());
    }
}

class Test {
    public int go()

    {
        boolean debug;
        int junk;
        junk = this.something();
        debug = false;
        if (debug) {
            junk = junk + 1;
        } else {
            junk = 0;
        }
        return junk + 2;
    }

    public int something() {
        return 3;
    }
}
