package j;

import j.parser.*;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static j.parser.TokenType.EOF;


/**
 * @author chuxiaofeng
 */
@SuppressWarnings("WeakerAccess")
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class Benchmark {

    String input = Helper.read(Helper.resource2path("/pattern.j"));
    static Lexer.SourceInput source(@NotNull String input) {
        return Lexer.source("<inline>", input);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public List<Token<TokenType>> a() {
        Lexer.TokenIterator<TokenType> iter = new Lexer.TokenIterator<>(new Lexer<>(new LexerRules(), EOF, source(input)));
        List<Token<TokenType>> lst = new ArrayList<>();
        while (iter.hasNext()) {
            lst.add(iter.next());
        }
        return lst;
    }

    @org.openjdk.jmh.annotations.Benchmark
    public List<Token<TokenType>> b() {
        Lexer.TokenIterator<TokenType> iter = new Lexer.TokenIterator<>(new Lexer<>(new LexerRules1(), EOF, source(input)));
        List<Token<TokenType>> lst = new ArrayList<>();
        while (iter.hasNext()) {
            lst.add(iter.next());
        }
        return lst;
    }

    public static void main(String[] args) throws RunnerException {
//        简单测试冷启动
//        new BenchmarkTest().a().size();
//        new BenchmarkTest().b().size();
//
//        long s = System.nanoTime();
//        System.out.println(new BenchmarkTest().b().size());
//        System.out.println((System.nanoTime() - s) / 1e6);
//
//        s = System.nanoTime();
//        System.out.println(new BenchmarkTest().a().size());
//        System.out.println((System.nanoTime() - s) / 1e6);



        Options opt = new OptionsBuilder()
                .include(Benchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .build();
        new Runner(opt).run();
    }
}
