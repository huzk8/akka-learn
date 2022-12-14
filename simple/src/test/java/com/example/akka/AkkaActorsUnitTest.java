package com.example.akka;

import akka.actor.*;
import akka.testkit.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static akka.pattern.PatternsCS.ask;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AkkaActorsUnitTest {

    private static ActorSystem system = null;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("test-system");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system, Duration.apply(1000, TimeUnit.MILLISECONDS), true);
        system = null;
    }

    @Test
    public void givenAnActor_sendHimAMessageUsingTell() {

        final TestKit probe = new TestKit(system);
        ActorRef logActorRef = probe.childActorOf(Props.create(LogAddressActor.class));
        logActorRef.tell("printit", probe.testActor());

        probe.expectMsg("Got Message");
    }

    @Test
    public void printer() {
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = testKit.childActorOf(Props.create(PrinterActor.class));
        int totalNumberOfWords = 111;
        actorRef.tell(new PrinterActor.PrintFinalResult(totalNumberOfWords), testKit.testActor());

        testKit.expectMsg(totalNumberOfWords + "个单词已收到");
    }

    @Test
    public void worldCount_Normal() throws ExecutionException, InterruptedException {
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = testKit.childActorOf(Props.create(WordCounterActor.class));
        CompletableFuture<Object> future = ask(actorRef, new WordCounterActor.CountWords("chinese man is good"), 20000).toCompletableFuture();
        Integer numberOfWords = (Integer) future.get();
        assertTrue("The actor should count 4 words", 4 == numberOfWords);
    }

    @Test
    public void worldCount_Exception() throws ExecutionException, InterruptedException {
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = testKit.childActorOf(Props.create(WordCounterActor.class));
        CompletableFuture<Object> future = ask(actorRef, new WordCounterActor.CountWords(null), 1000).toCompletableFuture();
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            // 接受者发生异常时，发送方也可以捕获并处理
            assertTrue("Invalid error message", e.getMessage().contains("The text to process can't be null!"));
        } catch (InterruptedException | TimeoutException e) {
            fail("Actor should respond with an exception instead of timing out !");
        }
    }

    @Test
    public void worldCount_Timeout() throws ExecutionException, InterruptedException {
        TestKit testKit = new TestKit(system);
        ActorRef actorRef = testKit.childActorOf(Props.create(WordCounterActor.class));
        CompletableFuture<Object> future = ask(actorRef, new WordCounterActor.CountWords(null, true), 1000).toCompletableFuture();
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            // 接受者发生异常时，发送方也可以捕获并处理
            assertTrue("Invalid error message", e.getMessage().contains("The text to process can't be null!"));
        } catch (InterruptedException | TimeoutException e) {
            fail("Actor should respond with an exception instead of timing out !");
        }
    }

    /**
     * 读取文章--》拆分单词计数--》 将计算结果打印
     */
    @Test
    public void givenAnAkkaSystem_countTheWordsInAText() {
        ActorSystem actorSystem = ActorSystem.create("my-system");
        ActorRef logActor = actorSystem.actorOf(Props.create(LogAddressActor.class), "log-actor");
        logActor.tell("printit", null);

        ActorRef readingActorRef = actorSystem.actorOf(ReadingActor.props(LINES), "reading-actor");
        readingActorRef.tell(new ReadingActor.ReadLines(), ActorRef.noSender()); //ActorRef.noSender() means the sender ref is akka://test-system/deadLetters

        // 当不再引用Actor时，它们不会自动停止。当我们不再需要一个Actor时，我们必须显式销毁它以防止内存泄漏
        logActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        // 发生kill命令stop actor，与PoisonPill不一样，它会抛出异常ActorKilledException
        readingActorRef.tell(Kill.getInstance(),ActorRef.noSender());
        // 不能直接停止系统，它不会等待所有actor执行完
//        Future<Terminated> terminateResponse = actorSystem.terminate();

    }

    /**
     * actor执行是按msg的发送顺序的，上一个没执行完不会执行后面的
     */
    @Test
    public void longTime() throws InterruptedException {
        ActorSystem actorSystem = ActorSystem.create("my-system");
        ActorRef longActor = actorSystem.actorOf(Props.create(LongTimeActor.class), "long-actor");
        longActor.tell(new LongTimeActor.ShortTimeTaskInfo(1,""), null);
        longActor.tell(new LongTimeActor.LongTimeTaskInfo(2,""), null);
        longActor.tell(new LongTimeActor.ShortTimeTaskInfo(3,""), null);
        longActor.tell(new LongTimeActor.ShortTimeTaskInfo(4,""), null);
        longActor.tell(new LongTimeActor.ShortTimeTaskInfo(5,""), null);

        Thread.sleep(15_000);

    }


    public static final String LINES = "Lorem Ipsum is simply dummy text\n" +
            "of the printing and typesetting industry.\n" +
            "Lorem Ipsum has been the industry's standard dummy text\n" +
            "ever since the 1500s, when an unknown printer took a galley\n" +
            "of type and scrambled it to make a type specimen book.\n" +
            " It has survived not only five centuries, but also the leap\n" +
            "into electronic typesetting, remaining essentially unchanged.\n" +
            " It was popularised in the 1960s with the release of Letraset\n" +
            " sheets containing Lorem Ipsum passages, and more recently with\n" +
            " desktop publishing software like Aldus PageMaker including\n" +
            "versions of Lorem Ipsum.";

}